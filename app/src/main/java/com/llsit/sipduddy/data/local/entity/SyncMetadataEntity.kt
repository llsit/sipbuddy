package com.llsit.sipduddy.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val ownerId: String,
    val lastSuccessAt: Long? = null,
    val error: String? = null,
    val conflicts: Int = 0,
)
