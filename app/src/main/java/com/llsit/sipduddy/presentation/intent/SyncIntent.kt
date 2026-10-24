package com.llsit.sipduddy.presentation.intent

sealed interface SyncIntent {
    data object SyncNow : SyncIntent
    data class ImportGuestHistory(val ownerId: String) : SyncIntent
}
