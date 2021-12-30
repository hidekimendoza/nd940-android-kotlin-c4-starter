package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun reminderDatabase_InsertedReminder_ShouldBeAbleToGetItByID() = runBlockingTest {
        val reminder = ReminderDTO(
            "Title1",
            "Description1", "Location1", 100.0, 100.0, "001"
        )

        database.reminderDao().saveReminder(reminder)
        val loaded = database.reminderDao().getReminderById(reminder.id)

        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun reminderDatabase_DeleteExistingReminders_getRemindersShouldBeEmpty() = runBlockingTest {
        val reminder1 = ReminderDTO(
            "Title1",
            "Description1", "Location1", 100.0, 100.0, "001"
        )
        val reminder2 = ReminderDTO(
            "Title2",
            "Description2", "Location2", 200.0, 200.0, "002"
        )

        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        var loaded = database.reminderDao().getReminders()

        assertThat(loaded[0].id, `is`(reminder1.id))
        assertThat(loaded[1].id, `is`(reminder2.id))
        assertThat(loaded.size, `is`(2))

        database.reminderDao().deleteAllReminders()
        loaded = database.reminderDao().getReminders()
        assertThat(loaded.isEmpty(), `is`(true))
    }

}