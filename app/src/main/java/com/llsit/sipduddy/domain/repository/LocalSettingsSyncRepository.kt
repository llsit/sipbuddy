package com.llsit.sipduddy.domain.repository

import com.llsit.sipduddy.domain.model.SettingsSyncRecord
import kotlinx.coroutines.flow.Flow

interface LocalSettingsSyncRepository {
    fun observeRecord(owner: String): Flow<SettingsSyncRecord>
    suspend fun read(owner: String): SettingsSyncRecord
    suspend fun acknowledge(sent: SettingsSyncRecord, remote: SettingsSyncRecord)
    suspend fun merge(remote: SettingsSyncRecord)
}
