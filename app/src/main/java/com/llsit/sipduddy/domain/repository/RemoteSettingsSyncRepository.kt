package com.llsit.sipduddy.domain.repository

import com.llsit.sipduddy.domain.model.SettingsSyncRecord
import com.llsit.sipduddy.domain.model.SettingsSyncPushResult

interface RemoteSettingsSyncRepository {
    suspend fun push(record: SettingsSyncRecord): SettingsSyncPushResult
    suspend fun read(owner: String): SettingsSyncRecord?
}
