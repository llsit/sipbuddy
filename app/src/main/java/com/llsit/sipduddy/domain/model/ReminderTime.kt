package com.llsit.sipduddy.domain.model

data class ReminderTime(val hour: Int = 10, val minute: Int = 0) {
    init {
        require(hour in 0..23) { "ชั่วโมงต้องอยู่ระหว่าง 0–23" }
        require(minute in 0..59) { "นาทีต้องอยู่ระหว่าง 0–59" }
    }
}
