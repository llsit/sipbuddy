package com.llsit.sipduddy.domain.repository
import com.llsit.sipduddy.domain.model.SyncRecord
interface LocalHydrationSyncRepository {
    suspend fun pending(owner: String, limit: Int): List<SyncRecord>
    suspend fun acknowledge(sent: SyncRecord, canonical: SyncRecord)
    suspend fun merge(owner: String, records: List<SyncRecord>)
}
