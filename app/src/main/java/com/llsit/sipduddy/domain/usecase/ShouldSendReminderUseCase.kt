package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.repository.DrinkRepository
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.ZoneId

class ShouldSendReminderUseCase(
    private val drinks: DrinkRepository,
    private val settings: HydrationSettingsRepository,
    private val clock: Clock,
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
) {
    suspend operator fun invoke(): Boolean {
        val preferences = settings.settings.first()
        val now = clock.instant().atZone(zoneProvider())
        val reminder = preferences.reminder
        if (!reminder.enabled || reminder.quietHours.contains(now.toLocalTime())) return false
        if (!reminder.stopAtGoal) return true
        val start = now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
        val end = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli()
        return drinks.getTotalMl(start, end) < preferences.goalMl
    }
}
