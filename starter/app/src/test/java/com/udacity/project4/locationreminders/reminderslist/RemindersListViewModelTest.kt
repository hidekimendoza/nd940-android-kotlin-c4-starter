package com.udacity.project4.locationreminders.reminderslist

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.util.MainCoroutineRule
import getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.test.AutoCloseKoinTest

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest : AutoCloseKoinTest() {

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private lateinit var dataSource: ReminderDataSource
    private lateinit var viewModel: RemindersListViewModel

    @Before
    fun setup() {
        stopKoin()
        dataSource = FakeDataSource()
        viewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            dataSource
        )
    }


    @Test
    fun loadReminders_withEmptyList_shouldSetShowNoData() = mainCoroutineRule.runBlockingTest {

        // Given an empty datasource
        dataSource.deleteAllReminders()
        // When loadReminders is called
        viewModel.loadReminders()
        val reminderList = viewModel.remindersList.getOrAwaitValue()
        val showNoData = viewModel.showNoData.getOrAwaitValue()
        assertThat(showNoData, `is`(true))
        assertThat(
            reminderList.isEmpty(),
            `is`(true)
        )
    }

    @Test
    fun loadReminder_withRemindersAdded_shouldUpdateReminderList() =
        mainCoroutineRule.runBlockingTest {
            // GIVEN a datasource with multiple reminders
            val r1 = ReminderDTO("Title1", "Description1", "Location1", 100.0, 100.0, "001")
            val r2 = ReminderDTO("Title2", "Description2", "Location2", 200.0, 200.0, "002")
            dataSource.saveReminder(r1)
            dataSource.saveReminder(r2)
            // WHEN loadReminders is called
            viewModel.loadReminders()
            val reminderList = viewModel.remindersList.getOrAwaitValue()
            val showNoData = viewModel.showNoData.getOrAwaitValue()
            // Reminder list should be updated with the reminders
            assertThat(showNoData, `is`(false))
            // assertThat(viewModel.s)
            assertThat(reminderList[0].id, `is`(r1.id))
            assertThat(reminderList[1].id, `is`(r2.id))

        }

    @Test
    fun loadReminder_withErrorGettingReminders_ShouldSnackBarErrorMessage() =
        mainCoroutineRule.runBlockingTest {
            // Given a datasource with errors
            (dataSource as FakeDataSource).sendError = true

            //WHEN load reminders is called
            viewModel.loadReminders()
            val showSnackBar = viewModel.showSnackBar.getOrAwaitValue()
            assertThat(showSnackBar, `is`("Error getting reminder"))


        }
}