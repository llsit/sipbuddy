package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.ReminderTime
import java.time.ZonedDateTime

class CalculateNextReminderUseCase {
    operator fun invoke(now: ZonedDateTime, time: ReminderTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(time.hour, time.minute).atZone(now.zone)
        return if (today.isAfter(now)) {
            today
        } else {
            now.toLocalDate().plusDays(1)
                .atTime(time.hour, time.minute).atZone(now.zone)
        }
    }
}
