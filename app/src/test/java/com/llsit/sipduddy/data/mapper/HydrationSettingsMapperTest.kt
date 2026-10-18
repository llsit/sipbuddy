package com.llsit.sipduddy.data.mapper

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.llsit.sipduddy.domain.model.HydrationSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class HydrationSettingsMapperTest {
    private val mapper = HydrationSettingsMapper()

    @Test
    fun emptyPreferencesHaveDefaults() {
        assertEquals(HydrationSettings(), mapper.toDomain(emptyPreferences()))
    }

    @Test
    fun invalidLegacyValuesAreSanitized() {
        val raw = mutablePreferencesOf(
            intPreferencesKey("goal_ml") to -1,
            intPreferencesKey("reminder_hour") to 99,
            intPreferencesKey("reminder_minute") to -3,
        )
        assertEquals(HydrationSettings(), mapper.toDomain(raw))
    }

    @Test fun legacyTimeIsPreservedWhenLoadingOldInstall() {
        val raw = mutablePreferencesOf(
            intPreferencesKey("reminder_hour") to 16,
            intPreferencesKey("reminder_minute") to 45,
            androidx.datastore.preferences.core.booleanPreferencesKey("reminder_enabled") to true,
        )
        val settings = mapper.toDomain(raw)
        assertEquals(listOf(com.llsit.sipduddy.domain.model.ReminderTime(16, 45)), settings.reminder.times)
        assertEquals(true, settings.reminder.enabled)
    }

    @Test fun multipleTimesAndQuietHoursRoundTripWithoutLosingUnrelatedValues() {
        val settings = HydrationSettings(
            3500,
            com.llsit.sipduddy.domain.model.ReminderSettings(
                true,
                listOf(com.llsit.sipduddy.domain.model.ReminderTime(8, 15), com.llsit.sipduddy.domain.model.ReminderTime(19, 30)),
                com.llsit.sipduddy.domain.model.QuietHours(
                    true,
                    com.llsit.sipduddy.domain.model.ReminderTime(21, 30),
                    com.llsit.sipduddy.domain.model.ReminderTime(6, 15),
                ),
                false,
            ),
        )
        val raw = mutablePreferencesOf(intPreferencesKey("unrelated") to 42)
        mapper.write(settings, raw)
        assertEquals(settings, mapper.toDomain(raw))
        assertEquals(42, raw[intPreferencesKey("unrelated")])
    }

    @Test fun corruptOrDuplicateSlotsAreSanitizedAndAllQuietScheduleIsDisabled() {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("reminder_times")
        val raw = mutablePreferencesOf(key to "99:00,broken,14:30,14:30,8:00,9:99")
        assertEquals(
            listOf(
                com.llsit.sipduddy.domain.model.ReminderTime(8, 0),
                com.llsit.sipduddy.domain.model.ReminderTime(14, 30),
            ),
            mapper.toDomain(raw).reminder.times,
        )
        raw[key] = "23:00"
        raw[androidx.datastore.preferences.core.booleanPreferencesKey("reminder_enabled")] = true
        assertEquals(false, mapper.toDomain(raw).reminder.enabled)
    }
}
