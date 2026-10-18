package com.llsit.sipduddy.domain.repository
import com.llsit.sipduddy.domain.model.SyncRecord
import com.llsit.sipduddy.domain.model.SyncPushResult
interface RemoteHydrationSyncRepository {
    suspend fun push(record: SyncRecord): SyncPushResult
    suspend fun page(owner: String, afterId: String?, limit: Int): List<SyncRecord>
}
