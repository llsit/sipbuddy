package com.llsit.sipduddy.data.local.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

internal object HydrationPreferenceKeys {
    val goal = intPreferencesKey("goal_ml")
    val reminderEnabled = booleanPreferencesKey("reminder_enabled")
    val reminderHour = intPreferencesKey("reminder_hour")
    val reminderMinute = intPreferencesKey("reminder_minute")
}
