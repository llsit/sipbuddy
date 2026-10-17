package com.llsit.sipduddy.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.llsit.sipduddy.data.mapper.HydrationSettingsMapper
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

class DataStoreHydrationSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val mapper: HydrationSettingsMapper,
) : HydrationSettingsRepository {
    override val settings = dataStore.data.map(mapper::toDomain).distinctUntilChanged()
    override suspend fun save(settings: HydrationSettings) {
        dataStore.edit { mapper.write(settings, it) }
    }
}
