package com.llsit.sipduddy.data.local.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.llsit.sipduddy.data.local.dao.DrinkDao
import com.llsit.sipduddy.data.local.dao.HydrationSyncDao
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import com.llsit.sipduddy.data.local.entity.SyncMetadataEntity
@Database(entities = [DrinkEntity::class, SyncMetadataEntity::class], version = 2, exportSchema = true)
abstract class HydrationDatabase : RoomDatabase() {
    abstract fun drinks(): DrinkDao
    abstract fun sync(): HydrationSyncDao
}
