package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.ReminderSettings
import java.time.LocalTime
import java.time.ZonedDateTime

class CalculateNextScheduledReminderUseCase {
    operator fun invoke(now: ZonedDateTime, settings: ReminderSettings): ZonedDateTime? {
        if (!settings.enabled) return null
        return (0L..7L).asSequence().flatMap { offset ->
            settings.times.asSequence().map { time ->
                now.toLocalDate().plusDays(offset).atTime(LocalTime.of(time.hour, time.minute)).atZone(now.zone)
            }
        }.filter { it.isAfter(now) && !settings.quietHours.contains(it.toLocalTime()) }
            .minByOrNull { it.toInstant() }
    }
}
