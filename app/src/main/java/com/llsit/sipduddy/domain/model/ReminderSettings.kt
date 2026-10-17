package com.llsit.sipduddy.domain.model

import java.time.LocalTime

data class ReminderSettings(
    val enabled: Boolean = false,
    val times: List<ReminderTime> = DEFAULT_TIMES,
    val quietHours: QuietHours = QuietHours(),
    val stopAtGoal: Boolean = true,
) {
    constructor(enabled: Boolean, time: ReminderTime) : this(enabled, listOf(time))
    val time: ReminderTime get() = times.first()
    init {
        require(times.isNotEmpty() && times.size <= MAX_TIMES && times.distinct().size == times.size)
        require(!enabled || times.any { !quietHours.contains(LocalTime.of(it.hour, it.minute)) }) {
            "ต้องมีเวลาเตือนอย่างน้อยหนึ่งช่วงอยู่นอกช่วงพัก"
        }
    }
    companion object {
        const val MAX_TIMES = 12
        val DEFAULT_TIMES = listOf(ReminderTime(10, 0), ReminderTime(14, 0), ReminderTime(18, 0))
    }
}
