package com.llsit.sipduddy.domain.repository

import com.llsit.sipduddy.domain.model.SettingsSyncPushResult
import com.llsit.sipduddy.domain.model.SettingsSyncRecord

interface RemoteSettingsSyncRepository {
    suspend fun push(record: SettingsSyncRecord): SettingsSyncPushResult
    suspend fun read(owner: String): SettingsSyncRecord?
}
