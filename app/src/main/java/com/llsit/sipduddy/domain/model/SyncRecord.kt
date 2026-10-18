package com.llsit.sipduddy.domain.model
data class SyncRecord(
    val ownerId: String,
    val remoteId: String,
    val drink: String,
    val amountMl: Int,
    val timestamp: Long,
    val deleted: Boolean,
    val serverVersion: Long,
    val operationId: String,
    val localVersion: Long = 0,
    val allowRestore: Boolean = false,
)
