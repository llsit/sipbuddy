package com.llsit.sipduddy.domain.reminder

import com.llsit.sipduddy.domain.model.ReminderSettings

interface ReminderScheduler { fun schedule(settings: ReminderSettings) }
