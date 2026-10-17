package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.reminder.ReminderNotificationPublisher

class HandleReminderUseCase(
    private val restoreReminderUseCase: RestoreReminderUseCase,
    private val publisher: ReminderNotificationPublisher,
    private val shouldSendReminderUseCase: ShouldSendReminderUseCase,
) {
    suspend operator fun invoke(shouldNotify: Boolean) {
        val settings = restoreReminderUseCase()
        if (shouldNotify && settings.enabled && shouldSendReminderUseCase()) publisher.show()
    }
}
