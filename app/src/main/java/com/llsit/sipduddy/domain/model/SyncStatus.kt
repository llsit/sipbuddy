package com.llsit.sipduddy.domain.model
data class SyncStatus(
    val ownerId: String = "",
    val pending: Int = 0,
    val guestCount: Int = 0,
    val isRunning: Boolean = false,
    val lastSuccessAt: Long? = null,
    val error: String? = null,
    val conflicts: Int = 0,
)
