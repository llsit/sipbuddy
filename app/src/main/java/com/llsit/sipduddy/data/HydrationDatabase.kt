package com.llsit.sipduddy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "drinks", indices = [Index("timestamp")])
data class DrinkEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val drink: String, val amountMl: Int, val timestamp: Long)

@Dao
interface DrinkDao {
    @Query("SELECT * FROM drinks ORDER BY timestamp DESC, id DESC")
    fun observe(): Flow<List<DrinkEntity>>
    @Insert suspend fun insert(entry: DrinkEntity)
    @Query("DELETE FROM drinks WHERE id = :id") suspend fun delete(id: Long)
}
@Database(entities = [DrinkEntity::class], version = 1, exportSchema = true)
abstract class HydrationDatabase : RoomDatabase() { abstract fun drinks(): DrinkDao }
