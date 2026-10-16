package com.llsit.sipduddy.domain.model

data class ReminderSettings(
    val enabled: Boolean = false,
    val time: ReminderTime = ReminderTime(),
)
