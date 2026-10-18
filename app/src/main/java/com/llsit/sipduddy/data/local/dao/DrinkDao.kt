package com.llsit.sipduddy.data.local.dao
import androidx.room.*
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface DrinkDao {
    @Query("SELECT * FROM drinks WHERE ownerId=:owner AND deleted=0 AND timestamp>=:startInclusive AND timestamp<:endExclusive ORDER BY timestamp DESC,id DESC")
    fun observeEntries(startInclusive: Long, endExclusive: Long, owner: String = ""): Flow<List<DrinkEntity>>
    @Query("SELECT COALESCE(SUM(amountMl),0) FROM drinks WHERE ownerId=:owner AND deleted=0 AND timestamp>=:startInclusive AND timestamp<:endExclusive")
    suspend fun getTotalMl(startInclusive: Long, endExclusive: Long, owner: String = ""): Long
    @Query("SELECT * FROM drinks WHERE id=:id AND ownerId=:owner AND deleted=0")
    suspend fun getById(id: Long, owner: String = ""): DrinkEntity?
    @Query("SELECT * FROM drinks WHERE id=:id AND ownerId=:owner")
    suspend fun getStored(id: Long, owner: String): DrinkEntity?
    @Insert suspend fun insert(entry: DrinkEntity)
    @Update suspend fun update(entry: DrinkEntity): Int
    @Query("UPDATE drinks SET deleted=1,pending=1,allowRestore=0,localVersion=localVersion+1,operationId=:operation WHERE id=:id AND ownerId=:owner AND deleted=0")
    suspend fun markDeleted(id: Long, owner: String, operation: String)
    suspend fun delete(id: Long, owner: String = "") = markDeleted(id, owner, UUID.randomUUID().toString())
    @Transaction
    suspend fun deleteAndGet(id: Long, owner: String = ""): DrinkEntity? {
        val entry = getById(id, owner) ?: return null
        delete(id, owner)
        return entry
    }
    @Transaction
    suspend fun editOwned(entry: DrinkEntity): Boolean {
        val old = getById(entry.id, entry.ownerId) ?: return false
        return update(old.copy(drink=entry.drink,amountMl=entry.amountMl,timestamp=entry.timestamp,
            localVersion=old.localVersion+1,pending=true,operationId=UUID.randomUUID().toString(),allowRestore=old.allowRestore)) == 1
    }
    @Transaction
    suspend fun restore(entry: DrinkEntity): Long {
        val old = getStored(entry.id, entry.ownerId)
        if (old != null && !old.deleted) return -1L
        if (old == null) insert(entry.copy(pending=true,allowRestore=true))
        else update(old.copy(drink=entry.drink,amountMl=entry.amountMl,timestamp=entry.timestamp,
            pending=true,deleted=false,localVersion=old.localVersion+1,operationId=UUID.randomUUID().toString(),allowRestore=true))
        return entry.id
    }
}
