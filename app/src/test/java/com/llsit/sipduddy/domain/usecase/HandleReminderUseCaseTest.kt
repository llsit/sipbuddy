package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.reminder.ReminderNotificationPublisher
import com.llsit.sipduddy.fake.FakeHydrationSettingsRepository
import com.llsit.sipduddy.fake.FakeReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HandleReminderUseCaseTest {
    @Test
    fun bootRestoresWithoutPostingAndDisabledAlarmsDoNotPost() = runBlocking {
        val repository = FakeHydrationSettingsRepository()
        val scheduler = FakeReminderScheduler()
        var notifications = 0
        val publisher = object : ReminderNotificationPublisher {
            override fun show() {
                notifications++
            }
        }
        val useCase =
            HandleReminderUseCase(
                RestoreReminderUseCase(repository, scheduler),
                publisher,
                ShouldSendReminderUseCase(
                    com.llsit.sipduddy.fake.FakeDrinkRepository(),
                    repository,
                    java.time.Clock.fixed(java.time.Instant.parse("2026-10-17T10:00:00Z"), java.time.ZoneOffset.UTC),
                    { java.time.ZoneOffset.UTC },
                ),
            )
        useCase(shouldNotify = true)
        repository.settings.value = HydrationSettings(reminder = ReminderSettings(enabled = true))
        useCase(shouldNotify = false)
        assertEquals(0, notifications)
        useCase(shouldNotify = true)
        assertEquals(1, notifications)
        assertEquals(3, scheduler.scheduled.size)
    }
}
