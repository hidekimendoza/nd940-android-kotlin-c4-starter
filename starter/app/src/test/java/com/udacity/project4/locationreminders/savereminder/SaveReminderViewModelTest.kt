package com.udacity.project4.locationreminders.savereminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.util.MainCoroutineRule
import getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var dataSource: ReminderDataSource

    @Before
    fun setup() {
        stopKoin()
        dataSource = FakeDataSource()
        viewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            dataSource
        )
    }

    @Test
    fun onClear_afterCalled_AllReminderInfoShouldBeNull() = mainCoroutineRule.runBlockingTest {

        // Given a stored reminder
        viewModel.reminderTitle.value = "Title"
        viewModel.reminderDescription.value = "Description"
        viewModel.reminderSelectedLocationStr.value = "SelectedLocation"
        viewModel.selectedPOI.value = PointOfInterest(LatLng(100.0, 200.0), "s1", "s2")
        viewModel.latitude.value = 100.0
        viewModel.longitude.value = 200.0

        // When loadReminders is called
        viewModel.onClear()
        val title = viewModel.reminderTitle.getOrAwaitValue()
        val description = viewModel.reminderDescription.getOrAwaitValue()
        val locationStr = viewModel.reminderSelectedLocationStr.getOrAwaitValue()
        val selectedPoi = viewModel.selectedPOI.getOrAwaitValue()
        val latitude = viewModel.latitude.getOrAwaitValue()
        val longitude = viewModel.longitude.getOrAwaitValue()

        MatcherAssert.assertThat(title.isNullOrEmpty(), `is`(true))
        MatcherAssert.assertThat(description.isNullOrEmpty(), `is`(true))
        MatcherAssert.assertThat(locationStr.isNullOrEmpty(), `is`(true))
        MatcherAssert.assertThat(selectedPoi == null, `is`(true))
        MatcherAssert.assertThat(latitude == null, `is`(true))
        MatcherAssert.assertThat(longitude == null, `is`(true))

    }

    @Test
    fun validateAndSaveReminder_invalidData_ShoulShowErrorMessageAndNotUpdateDB() =
        mainCoroutineRule.runBlockingTest {
            // GIVEN an invalid reminder and an empty DB
            dataSource.deleteAllReminders()
            val invalidReminderTitle =
                ReminderDataItem(null, "description1", "location1", 100.0, 200.0, "001")
            val invalidReminderLocation =
                ReminderDataItem("title2", "description2", null, 300.0, 400.0, "002")

            // WHEN validateAndSaveReminder
            viewModel.validateAndSaveReminder(invalidReminderTitle)
            var snackbarMsg = viewModel.showSnackBarInt.getOrAwaitValue()
            MatcherAssert.assertThat(snackbarMsg, `is`(R.string.err_enter_title))

            // Then no reminders should be added and error at snackbarMsg
            viewModel.validateAndSaveReminder(invalidReminderLocation)
            snackbarMsg = viewModel.showSnackBarInt.getOrAwaitValue()
            MatcherAssert.assertThat(snackbarMsg, `is`(R.string.err_select_location))
            val reminders = dataSource.getReminders() as Result.Success<List<ReminderDTO>>
            MatcherAssert.assertThat(reminders.data.isEmpty(), `is`(true))
        }

    @Test
    fun validateAndSaveReminder_validData_ShouldReturnTrue() = mainCoroutineRule.runBlockingTest {
        dataSource.deleteAllReminders()
        val validReminder =
            ReminderDataItem("title1", "description1", "location1", 100.0, 200.0, "001")

        viewModel.validateAndSaveReminder(validReminder)

        val loading = viewModel.showLoading.getOrAwaitValue()
        val toast = viewModel.showToast.getOrAwaitValue()
        val nav = viewModel.navigationCommand.getOrAwaitValue()

        MatcherAssert.assertThat(loading, `is`(false))
        MatcherAssert.assertThat(toast, `is`("Reminder Saved !"))
        MatcherAssert.assertThat(nav, `is`(NavigationCommand.Back))

        val obtainedReminder = dataSource.getReminders() as Result.Success<List<ReminderDTO>>
        MatcherAssert.assertThat(obtainedReminder.data.size, `is`(1))

        MatcherAssert.assertThat(obtainedReminder.data[0].title, `is`(validReminder.title))
        MatcherAssert.assertThat(
            obtainedReminder.data[0].description,
            `is`(validReminder.description)
        )
        MatcherAssert.assertThat(obtainedReminder.data[0].location, `is`(validReminder.location))
        MatcherAssert.assertThat(obtainedReminder.data[0].latitude, `is`(validReminder.latitude))
        MatcherAssert.assertThat(obtainedReminder.data[0].longitude, `is`(validReminder.longitude))


    }

}