package com.llsit.sipduddy.fake

import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.reminder.ReminderScheduler

class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<ReminderSettings>()
    var failure: Exception? = null
    override fun schedule(settings: ReminderSettings) {
        failure?.let { throw it }
        scheduled += settings
    }
}
