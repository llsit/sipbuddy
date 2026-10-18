package com.llsit.sipduddy.domain.repository
import com.llsit.sipduddy.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
interface HydrationSyncRepository {
    val status: Flow<SyncStatus>
    fun requestSync()
    suspend fun importGuestHistory(expectedOwner: String): Int
}
