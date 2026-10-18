package com.llsit.sipduddy.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.reminder.ReminderScheduler
import com.llsit.sipduddy.domain.usecase.CalculateNextScheduledReminderUseCase
import java.time.Clock
import java.time.ZoneId

class AndroidReminderScheduler(
    private val context: Context,
    private val clock: Clock,
    private val calculateNextReminderUseCase: CalculateNextScheduledReminderUseCase,
) : ReminderScheduler {
    override fun schedule(settings: ReminderSettings) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            10,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (!settings.enabled) {
            manager.cancel(pendingIntent)
            return
        }
        val next = calculateNextReminderUseCase(
            clock.instant().atZone(ZoneId.systemDefault()),
            settings,
        ) ?: run {
            manager.cancel(pendingIntent)
            return
        }
        // Replaces the same PendingIntent without cancelling the existing alarm before a successful update.
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            pendingIntent,
        )
    }
}
