package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.reminder.ReminderScheduler
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import kotlinx.coroutines.flow.first

class RestoreReminderUseCase(
    private val repository: HydrationSettingsRepository,
    private val scheduler: ReminderScheduler,
    private val accounts: com.llsit.sipduddy.domain.repository.AccountRepository? = null,
) {
    suspend operator fun invoke(): ReminderSettings =
        repository.settings.first().let { settings ->
            if (accounts != null && settings.ownerId != accounts.owner.value) settings.reminder.copy(enabled = false)
            else settings.reminder.also(scheduler::schedule)
        }
}
