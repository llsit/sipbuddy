package com.llsit.sipduddy.data.local.dao

import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Query("SELECT * FROM drinks WHERE timestamp >= :startInclusive AND timestamp < :endExclusive ORDER BY timestamp DESC, id DESC")
    fun observeEntries(startInclusive: Long, endExclusive: Long): Flow<List<DrinkEntity>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM drinks WHERE timestamp >= :startInclusive AND timestamp < :endExclusive")
    suspend fun getTotalMl(startInclusive: Long, endExclusive: Long): Long

    @Query("SELECT * FROM drinks WHERE id = :id") suspend fun getById(id: Long): DrinkEntity?
    @Update suspend fun update(entry: DrinkEntity): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun restore(entry: DrinkEntity): Long

    @Transaction
    suspend fun deleteAndGet(id: Long): DrinkEntity? {
        val entry = getById(id) ?: return null
        delete(id)
        return entry
    }

    @Insert suspend fun insert(entry: DrinkEntity)
    @Query("DELETE FROM drinks WHERE id = :id") suspend fun delete(id: Long)
}
