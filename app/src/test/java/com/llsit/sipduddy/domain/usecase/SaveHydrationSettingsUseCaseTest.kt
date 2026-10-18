package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.model.ReminderTime
import com.llsit.sipduddy.domain.model.SaveHydrationSettingsResult
import com.llsit.sipduddy.fake.FakeHydrationSettingsRepository
import com.llsit.sipduddy.fake.FakeReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SaveHydrationSettingsUseCaseTest {
    @Test
    fun savesAndSchedulesIncludingDisable() = runBlocking {
        val repository = FakeHydrationSettingsRepository()
        val scheduler = FakeReminderScheduler()
        val useCase = SaveHydrationSettingsUseCase(repository, scheduler)
        val settings = HydrationSettings(2500, ReminderSettings(true, ReminderTime(9, 30)))
        assertEquals(SaveHydrationSettingsResult.SAVED, useCase(settings))
        assertEquals(settings, repository.settings.value)
        useCase(settings.copy(reminder = settings.reminder.copy(enabled = false)))
        assertFalse(scheduler.scheduled.last().enabled)
    }

    @Test
    fun repositoryFailureDoesNotSchedule() = runBlocking {
        val repository =
            FakeHydrationSettingsRepository().apply { saveFailure = IOException("disk full") }
        val scheduler = FakeReminderScheduler()
        try {
            SaveHydrationSettingsUseCase(
                repository,
                scheduler
            )(HydrationSettings(3000)); fail("failure swallowed")
        } catch (_: IOException) {
        }
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun schedulerFailureReturnsPartialSuccessAndKeepsPersistedSettings() = runBlocking {
        val repository = FakeHydrationSettingsRepository()
        val scheduler = FakeReminderScheduler().apply { failure = SecurityException("denied") }
        val settings = HydrationSettings(3000)
        assertEquals(
            SaveHydrationSettingsResult.SAVED_WITH_REMINDER_ERROR,
            SaveHydrationSettingsUseCase(repository, scheduler)(settings)
        )
        assertEquals(settings, repository.settings.value)
    }

    @Test
    fun cancellationIsPropagated() = runBlocking {
        val scheduler = FakeReminderScheduler().apply { failure = CancellationException() }
        try {
            SaveHydrationSettingsUseCase(FakeHydrationSettingsRepository(), scheduler)(
                HydrationSettings()
            ); fail("cancellation swallowed")
        } catch (_: CancellationException) {
        }
    }

    @Test
    fun invalidSettingsCannotBeConstructed() {
        for (goal in listOf(
            0,
            249,
            10001
        )) assertThrows(IllegalArgumentException::class.java) { HydrationSettings(goal) }
        assertThrows(IllegalArgumentException::class.java) { ReminderTime(24, 0) }
        assertThrows(IllegalArgumentException::class.java) { ReminderTime(10, 60) }
    }

    @Test fun accountSwitchAfterPersistenceCannotScheduleOldAccountAlarm() = runBlocking {
        val accounts = com.llsit.sipduddy.fake.FakeAccountRepository("A")
        val repository = object : com.llsit.sipduddy.domain.repository.HydrationSettingsRepository {
            override val settings = kotlinx.coroutines.flow.flowOf(HydrationSettings())
            override suspend fun save(settings: HydrationSettings) { accounts.owner.value = "B" }
        }
        val scheduler = FakeReminderScheduler()
        SaveHydrationSettingsUseCase(repository, scheduler, accounts)(HydrationSettings(3000), "A")
        assertTrue(scheduler.scheduled.isEmpty())
    }
}
