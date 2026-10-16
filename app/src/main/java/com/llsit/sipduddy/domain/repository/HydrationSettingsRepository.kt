package com.llsit.sipduddy.domain.repository

import com.llsit.sipduddy.domain.model.HydrationSettings
import kotlinx.coroutines.flow.Flow

interface HydrationSettingsRepository {
    val settings: Flow<HydrationSettings>
    suspend fun save(settings: HydrationSettings)
}
