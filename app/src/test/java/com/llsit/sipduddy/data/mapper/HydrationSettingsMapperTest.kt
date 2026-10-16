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
            intPreferencesKey("reminder_minute") to -3
        )
        assertEquals(HydrationSettings(), mapper.toDomain(raw))
    }
}
