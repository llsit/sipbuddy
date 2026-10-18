package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.repository.DrinkRepository
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.flow.first

class ShouldSendReminderUseCase(
    private val drinks: DrinkRepository,
    private val settings: HydrationSettingsRepository,
    private val clock: Clock,
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val accounts: com.llsit.sipduddy.domain.repository.AccountRepository? = null,
) {
    suspend operator fun invoke(): Boolean {
        val preferences = settings.settings.first()
        if (accounts != null && preferences.ownerId != accounts.owner.value) return false
        val now = clock.instant().atZone(zoneProvider())
        val reminder = preferences.reminder
        if (!reminder.enabled || reminder.quietHours.contains(now.toLocalTime())) return false
        if (!reminder.stopAtGoal) return true
        val start = now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
        val end = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli()
        val total = drinks.getTotalMl(start, end)
        return (accounts == null || preferences.ownerId == accounts.owner.value) && total < preferences.goalMl
    }
}
