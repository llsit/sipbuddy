package com.llsit.sipduddy.fake

import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.repository.HydrationSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeHydrationSettingsRepository : HydrationSettingsRepository {
    override val settings = MutableStateFlow(HydrationSettings())
    var saveFailure: Exception? = null
    override suspend fun save(settings: HydrationSettings) {
        saveFailure?.let { throw it }
        this.settings.value = settings
    }
}
