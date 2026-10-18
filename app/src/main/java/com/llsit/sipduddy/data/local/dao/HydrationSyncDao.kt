package com.llsit.sipduddy.data.local.dao
import androidx.room.*
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import com.llsit.sipduddy.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface HydrationSyncDao {
    @Query("SELECT * FROM drinks WHERE ownerId=:owner AND pending=1 ORDER BY id LIMIT :limit")
    suspend fun pending(owner: String, limit: Int): List<DrinkEntity>
    @Query("SELECT * FROM drinks WHERE ownerId=:owner AND remoteId=:remoteId")
    suspend fun find(owner: String, remoteId: String): DrinkEntity?
    @Query("SELECT COUNT(*) FROM drinks WHERE ownerId=:owner AND pending=1")
    fun observePendingCount(owner: String): Flow<Int>
    @Query("SELECT COALESCE(SUM(localVersion),0) FROM drinks WHERE ownerId=:owner")
    fun observeMutationVersion(owner: String): Flow<Long>
    @Query("SELECT COUNT(*) FROM drinks WHERE ownerId='' AND deleted=0")
    fun observeGuestCount(): Flow<Int>
    @Query("SELECT * FROM drinks WHERE ownerId='' AND deleted=0 LIMIT 200")
    suspend fun guests(): List<DrinkEntity>
    @Query("SELECT * FROM sync_metadata WHERE ownerId=:owner")
    fun observeMetadata(owner: String): Flow<SyncMetadataEntity?>
    @Query("SELECT * FROM sync_metadata WHERE ownerId=:owner")
    suspend fun metadata(owner: String): SyncMetadataEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun setMetadata(value: SyncMetadataEntity)
}
