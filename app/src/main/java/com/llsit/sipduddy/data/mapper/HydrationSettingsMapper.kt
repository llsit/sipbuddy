package com.llsit.sipduddy.data.mapper

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.QuietHours
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.model.ReminderTime
import java.time.LocalTime
import com.llsit.sipduddy.data.local.preferences.HydrationPreferenceKeys as Keys

class HydrationSettingsMapper {
    fun toDomain(preferences: Preferences): HydrationSettings {
        val goal = preferences[Keys.goal]?.takeIf { it in HydrationSettings.MIN_GOAL_ML..HydrationSettings.MAX_GOAL_ML }
            ?: HydrationSettings.DEFAULT_GOAL_ML
        val legacyHour = preferences[Keys.reminderHour]?.takeIf { it in 0..23 }
        val legacyMinute = preferences[Keys.reminderMinute]?.takeIf { it in 0..59 }
        val parsed = preferences[Keys.reminderTimes]?.split(',')?.mapNotNull { token ->
            val parts = token.split(':')
            val hour = parts.getOrNull(0)?.toIntOrNull()
            val minute = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && hour != null && hour in 0..23 && minute != null && minute in 0..59) {
                ReminderTime(hour, minute)
            } else {
                null
            }
        }?.distinct()?.sortedBy { it.hour * 60 + it.minute }?.take(ReminderSettings.MAX_TIMES)
        val times = parsed?.takeIf { it.isNotEmpty() } ?: if (legacyHour != null || legacyMinute != null) {
            listOf(ReminderTime(legacyHour ?: 10, legacyMinute ?: 0))
        } else {
            ReminderSettings.DEFAULT_TIMES
        }

        fun readTime(key: Preferences.Key<Int>, fallback: ReminderTime): ReminderTime {
            val minutes = preferences[key]?.takeIf { it in 0..1439 } ?: return fallback
            return ReminderTime(minutes / 60, minutes % 60)
        }

        val quiet = QuietHours(
            preferences[Keys.quietEnabled] ?: true,
            readTime(Keys.quietStart, ReminderTime(22, 0)),
            readTime(Keys.quietEnd, ReminderTime(7, 0)),
        )
        val enabled = preferences[Keys.reminderEnabled] == true && times.any { !quiet.contains(LocalTime.of(it.hour, it.minute)) }
        return HydrationSettings(goal, ReminderSettings(enabled, times, quiet, preferences[Keys.stopAtGoal] ?: true))
    }

    fun write(settings: HydrationSettings, preferences: MutablePreferences) {
        val reminder = settings.reminder
        preferences[Keys.goal] = settings.goalMl
        preferences[Keys.reminderEnabled] = reminder.enabled
        preferences[Keys.reminderTimes] = reminder.times.joinToString(",") { "${it.hour}:${it.minute}" }
        preferences[Keys.reminderHour] = reminder.time.hour
        preferences[Keys.reminderMinute] = reminder.time.minute
        preferences[Keys.quietEnabled] = reminder.quietHours.enabled
        preferences[Keys.quietStart] = reminder.quietHours.start.hour * 60 + reminder.quietHours.start.minute
        preferences[Keys.quietEnd] = reminder.quietHours.end.hour * 60 + reminder.quietHours.end.minute
        preferences[Keys.stopAtGoal] = reminder.stopAtGoal
    }
}
