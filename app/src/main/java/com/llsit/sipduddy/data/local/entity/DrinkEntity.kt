package com.llsit.sipduddy.data.local.entity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "drinks", indices = [Index("timestamp"), Index(value = ["ownerId", "remoteId"], unique = true), Index(value = ["ownerId", "pending"])])
data class DrinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drink: String,
    val amountMl: Int,
    val timestamp: Long,
    @ColumnInfo(defaultValue = "''") val ownerId: String = "",
    @ColumnInfo(defaultValue = "''") val remoteId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val serverVersion: Long = 0,
    @ColumnInfo(defaultValue = "0") val localVersion: Long = 1,
    @ColumnInfo(defaultValue = "1") val pending: Boolean = true,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
    @ColumnInfo(defaultValue = "''") val operationId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val allowRestore: Boolean = false,
)
