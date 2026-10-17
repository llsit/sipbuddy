package com.llsit.sipduddy.domain.model

import java.time.LocalTime

data class QuietHours(
    val enabled: Boolean = true,
    val start: ReminderTime = ReminderTime(22, 0),
    val end: ReminderTime = ReminderTime(7, 0),
) {
    // Half-open interval: the end time may receive reminders. Equal bounds mean no quiet interval.
    fun contains(time: LocalTime): Boolean {
        if (!enabled || start == end) return false
        val minute = time.hour * 60 + time.minute
        val from = start.hour * 60 + start.minute
        val until = end.hour * 60 + end.minute
        return if (from < until) minute in from until until else minute >= from || minute < until
    }
}
