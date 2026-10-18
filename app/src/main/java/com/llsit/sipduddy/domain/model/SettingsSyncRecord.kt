package com.llsit.sipduddy.domain.model

data class SettingsSyncRecord(
    val ownerId: String,
    val settings: HydrationSettings,
    val serverVersion: Long = 0,
    val localVersion: Long = 0,
    val operationId: String = "",
    val pending: Boolean = false,
)
