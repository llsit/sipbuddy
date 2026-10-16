package com.llsit.sipduddy.data.repository

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.llsit.sipduddy.data.mapper.HydrationSettingsMapper
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.model.ReminderTime
import com.llsit.sipduddy.fake.FakePreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class DataStoreHydrationSettingsRepositoryTest {
    @Test
    fun savesExistingKeysAndPreservesUnrelatedPreferences() = runBlocking {
        val store = FakePreferencesDataStore()
        store.data.value = preferencesOf(stringPreferencesKey("unrelated") to "keep")
        val repository = DataStoreHydrationSettingsRepository(store, HydrationSettingsMapper())
        val settings = HydrationSettings(3000, ReminderSettings(true, ReminderTime(9, 45)))
        repository.save(settings)
        assertEquals(settings, repository.settings.first())
        assertEquals("keep", store.data.value[stringPreferencesKey("unrelated")])
        assertEquals(3000, store.data.value[intPreferencesKey("goal_ml")])
    }

    @Test
    fun storageErrorsArePropagatedWithoutChangingSettings() = runBlocking {
        val store = FakePreferencesDataStore().apply { failure = IOException("disk full") }
        val repository = DataStoreHydrationSettingsRepository(store, HydrationSettingsMapper())
        try {
            repository.save(HydrationSettings(3000)); fail("error swallowed")
        } catch (_: IOException) {
        }
        assertEquals(HydrationSettings(), repository.settings.first())
    }
}
