package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.model.ReminderTime
import com.llsit.sipduddy.fake.FakeHydrationSettingsRepository
import com.llsit.sipduddy.fake.FakeReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreReminderUseCaseTest {
    @Test
    fun restoresLatestPersistedSettings() = runBlocking {
        val repository = FakeHydrationSettingsRepository()
        val scheduler = FakeReminderScheduler()
        repository.settings.value =
            HydrationSettings(reminder = ReminderSettings(true, ReminderTime(15, 45)))
        val reminder = RestoreReminderUseCase(repository, scheduler)()
        assertEquals(repository.settings.value.reminder, reminder)
        assertEquals(listOf(reminder), scheduler.scheduled)
    }
}
