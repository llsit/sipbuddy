package com.llsit.sipduddy.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.llsit.sipduddy.data.local.dao.DrinkDao
import com.llsit.sipduddy.data.local.entity.DrinkEntity

@Database(entities = [DrinkEntity::class], version = 1, exportSchema = true)
abstract class HydrationDatabase : RoomDatabase() { abstract fun drinks(): DrinkDao }
