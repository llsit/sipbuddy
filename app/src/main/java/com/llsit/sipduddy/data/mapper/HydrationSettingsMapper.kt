package com.llsit.sipduddy.data.mapper

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.llsit.sipduddy.data.local.preferences.HydrationPreferenceKeys
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.model.ReminderTime

class HydrationSettingsMapper {
    fun toDomain(preferences: Preferences): HydrationSettings {
        val goal = preferences[HydrationPreferenceKeys.goal]
            ?.takeIf { it in HydrationSettings.MIN_GOAL_ML..HydrationSettings.MAX_GOAL_ML }
            ?: HydrationSettings.DEFAULT_GOAL_ML
        val hour = preferences[HydrationPreferenceKeys.reminderHour]?.takeIf { it in 0..23 } ?: 10
        val minute =
            preferences[HydrationPreferenceKeys.reminderMinute]?.takeIf { it in 0..59 } ?: 0
        return HydrationSettings(
            goal, ReminderSettings(
                preferences[HydrationPreferenceKeys.reminderEnabled] ?: false,
                ReminderTime(hour, minute),
            )
        )
    }

    fun write(settings: HydrationSettings, preferences: MutablePreferences) {
        preferences[HydrationPreferenceKeys.goal] = settings.goalMl
        preferences[HydrationPreferenceKeys.reminderEnabled] = settings.reminder.enabled
        preferences[HydrationPreferenceKeys.reminderHour] = settings.reminder.time.hour
        preferences[HydrationPreferenceKeys.reminderMinute] = settings.reminder.time.minute
    }
}
