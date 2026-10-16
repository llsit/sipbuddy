package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.reminder.ReminderScheduler
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import kotlinx.coroutines.flow.first

class RestoreReminderUseCase(
    private val repository: HydrationSettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(): ReminderSettings =
        repository.settings.first().reminder.also(scheduler::schedule)
}
