package com.llsit.sipduddy.data.repository

import androidx.room.withTransaction
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.data.mapper.SyncRecordMapper
import com.llsit.sipduddy.domain.model.SyncRecord
import com.llsit.sipduddy.domain.repository.LocalHydrationSyncRepository

class RoomHydrationSyncRepository(private val database: HydrationDatabase, private val mapper: SyncRecordMapper) : LocalHydrationSyncRepository {
    private val dao = database.sync()
    private val drinks = database.drinks()
    override suspend fun pending(owner: String, limit: Int) = dao.pending(owner, limit).map(mapper::toDomain)
    override suspend fun acknowledge(sent: SyncRecord, canonical: SyncRecord) = database.withTransaction {
        require(sent.ownerId == canonical.ownerId && sent.remoteId == canonical.remoteId)
        val current = dao.find(sent.ownerId, sent.remoteId) ?: return@withTransaction
        if (current.serverVersion > canonical.serverVersion) return@withTransaction
        if (current.localVersion == sent.localVersion && current.operationId == sent.operationId) {
            drinks.update(mapper.toEntity(canonical, current.id, current.localVersion))
        } else {
            // A newer local edit happened while HTTP was in flight. Preserve it and only advance its base.
            drinks.update(current.copy(serverVersion = canonical.serverVersion))
        }
        Unit
    }

    override suspend fun merge(owner: String, records: List<SyncRecord>) = database.withTransaction {
        require(records.all { it.ownerId == owner })
        records.forEach { remote ->
            val current = dao.find(owner, remote.remoteId)
            if (current == null) {
                drinks.insert(mapper.toEntity(remote))
            } else if (!current.pending && remote.serverVersion > current.serverVersion) {
                drinks.update(mapper.toEntity(remote, current.id, current.localVersion))
            }
        }
    }
}
