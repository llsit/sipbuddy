package com.llsit.sipduddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Query("SELECT * FROM drinks WHERE timestamp >= :startInclusive AND timestamp < :endExclusive ORDER BY timestamp DESC, id DESC")
    fun observeEntries(startInclusive: Long, endExclusive: Long): Flow<List<DrinkEntity>>

    @Insert suspend fun insert(entry: DrinkEntity)
    @Query("DELETE FROM drinks WHERE id = :id") suspend fun delete(id: Long)
}
