package com.llsit.sipduddy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "drinks", indices = [Index("timestamp")])
data class DrinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drink: String,
    val amountMl: Int,
    val timestamp: Long,
)
