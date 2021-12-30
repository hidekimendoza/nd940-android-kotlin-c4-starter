package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@SmallTest
class RemindersLocalRepositoryTest {
    private lateinit var localDataRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataRepository =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @Test
    fun getReminders_withDBContainingReminders_shouldReturnReminders() = runBlocking {
        val reminder1 = ReminderDTO(
            "Title1",
            "Description1", "Location1", 100.0, 100.0, "001"
        )
        val reminder2 = ReminderDTO(
            "Title2",
            "Description2", "Location2", 200.0, 200.0, "002"
        )

        localDataRepository.saveReminder(reminder1)
        localDataRepository.saveReminder(reminder2)

        val result: Result<List<ReminderDTO>> = localDataRepository.getReminders()

        result as Result.Success
        assertThat(result.data.isNotEmpty(), `is`(true))
        assertThat(result.data.size, `is`(2))
        assertThat(result.data[0].id, `is`(reminder1.id))
        assertThat(result.data[0].title, `is`(reminder1.title))
        assertThat(result.data[0].description, `is`(reminder1.description))
        assertThat(result.data[0].location, `is`(reminder1.location))
        assertThat(result.data[0].latitude, `is`(reminder1.latitude))
        assertThat(result.data[0].longitude, `is`(reminder1.longitude))

        assertThat(result.data[1].id, `is`(reminder2.id))
        assertThat(result.data[1].title, `is`(reminder2.title))
        assertThat(result.data[1].description, `is`(reminder2.description))
        assertThat(result.data[1].location, `is`(reminder2.location))
        assertThat(result.data[1].latitude, `is`(reminder2.latitude))
        assertThat(result.data[1].longitude, `is`(reminder2.longitude))
    }

    @Test
    fun getReminderById_withDBContainingReminders_shouldReturnSpecifiedReminders() = runBlocking {
        val reminder1 = ReminderDTO(
            "Title1",
            "Description1", "Location1", 100.0, 100.0, "001"
        )
        val reminder2 = ReminderDTO(
            "Title2",
            "Description2", "Location2", 200.0, 200.0, "002"
        )

        localDataRepository.saveReminder(reminder1)
        localDataRepository.saveReminder(reminder2)

        val result: Result<ReminderDTO> = localDataRepository.getReminder(reminder2.id)

        result as Result.Success
        assertThat(result.data.id, `is`(reminder2.id))
        assertThat(result.data.title, `is`(reminder2.title))
        assertThat(result.data.description, `is`(reminder2.description))
        assertThat(result.data.location, `is`(reminder2.location))
        assertThat(result.data.latitude, `is`(reminder2.latitude))
        assertThat(result.data.longitude, `is`(reminder2.longitude))
    }

    @Test
    fun deleteAllReminders_withDBContainingReminders_shouldReturnEmptyReminderList() = runBlocking {
        val reminder1 = ReminderDTO(
            "Title1",
            "Description1", "Location1", 100.0, 100.0, "001"
        )
        val reminder2 = ReminderDTO(
            "Title2",
            "Description2", "Location2", 200.0, 200.0, "002"
        )

        localDataRepository.saveReminder(reminder1)
        localDataRepository.saveReminder(reminder2)

        var result = localDataRepository.getReminders()

        result as Result.Success
        assertThat(result.data.isNotEmpty(), `is`(true))

        localDataRepository.deleteAllReminders()

        result = localDataRepository.getReminders()
        result as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }

    @Test
    fun getReminders_withUnstoredId_shoudReturnError() = runBlocking {

        var result = localDataRepository.getReminder("Not Added Id")
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @After
    fun closeDB() = database.close()

}