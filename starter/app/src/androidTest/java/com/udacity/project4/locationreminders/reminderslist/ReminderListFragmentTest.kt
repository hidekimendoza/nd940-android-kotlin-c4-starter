package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.annotation.NonNull
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun setup() {
        // Stop koin with app set DI
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            //Declare singleton definitions to be later injected using by inject()
            single {
                //This view model is declared singleton to be used across multiple fragments
                SaveReminderViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }

        startKoin {
            androidContext(appContext)
            modules(listOf(myModule))
            repository = get()
            runBlocking { repository.deleteAllReminders() }

        }
    }

    @Test
    fun addReminder_onClickAddReminderButton_navigateToSaveReminderFragment() {
        // GIVEN the Reminder List Fragment
        val navController = mock(NavController::class.java)
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN pressing addReminder FAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun reminderListFragment_withEmptyListOfReminders_shouldDisplayIcon() {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

    }

    @Test
    fun reminderListFragment_multipleReminders_shouldDisplayList() {
        runBlocking {
            repository.saveReminder(
                ReminderDTO("Title1", "Description1", "Location1", 100.0, 100.0)
            )
            repository.saveReminder(
                ReminderDTO("Title2", "Description2", "Location2", 200.0, 200.0, "002")
            )
        }
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, withText("Title1"), R.id.title)))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, withText("Description1"), R.id.description)))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(0, withText("Location1"), R.id.location)))
        onView(withId(R.id.reminderssRecyclerView))

            .check(matches(atPosition(1, withText("Title2"), R.id.title)))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(1, withText("Description2"), R.id.description)))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(atPosition(1, withText("Location2"), R.id.location)))

    }

}

fun atPosition(
    position: Int, itemMatcher: Matcher<View?>,
    @NonNull targetViewId: Int
): Matcher<View?>? {
    return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has view id $itemMatcher at position $position")
        }

        override fun matchesSafely(recyclerView: RecyclerView): Boolean {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
            val targetView: View = viewHolder!!.itemView.findViewById(targetViewId)
            return itemMatcher.matches(targetView)
        }
    }
}