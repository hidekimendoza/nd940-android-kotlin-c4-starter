package com.udacity.project4.locationreminders.data

import androidx.lifecycle.MutableLiveData
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var sendError: Boolean = false

    private val remindersData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()
    private val observableReminders = MutableLiveData<Result<List<ReminderDTO>>>()

//    TODO: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (sendError) {
            return Result.Error("Error getting reminder")
        }
        return Result.Success(remindersData.values.toList())
        // ("Return the reminders")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersData[reminder.id] = reminder
        updateReminderList()
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO>{
        if (!sendError){
            try {
                val reminder = remindersData.get(id)
                if (reminder != null) {
                    return Result.Success(reminder)
                } else {
                    return Result.Error("Reminder not found!")
                }
            } catch (e: Exception) {
                return Result.Error(e.localizedMessage)
            }
        }
        else
        {
            return Result.Error("Throwed exception")
        }
    }

    override suspend fun deleteAllReminders() {
        remindersData.clear()
        updateReminderList()
    }

    // function to modify state of repository
    fun addTask(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersData[reminder.id] = reminder
        }
        runBlocking { updateReminderList() }
    }

    private suspend fun updateReminderList() {
        observableReminders.value = getReminders()
    }


}