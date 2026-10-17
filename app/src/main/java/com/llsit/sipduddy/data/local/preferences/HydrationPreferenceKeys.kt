package com.llsit.sipduddy.data.local.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

internal object HydrationPreferenceKeys {
    val reminderTimes = androidx.datastore.preferences.core.stringPreferencesKey("reminder_times")
    val quietEnabled = booleanPreferencesKey("quiet_enabled")
    val quietStart = intPreferencesKey("quiet_start_minutes")
    val quietEnd = intPreferencesKey("quiet_end_minutes")
    val stopAtGoal = booleanPreferencesKey("stop_at_goal")
    val goal = intPreferencesKey("goal_ml")
    val reminderEnabled = booleanPreferencesKey("reminder_enabled")
    val reminderHour = intPreferencesKey("reminder_hour")
    val reminderMinute = intPreferencesKey("reminder_minute")
}
