package com.llsit.sipduddy.presentation.mutation

import com.llsit.sipduddy.domain.model.SyncStatus

internal sealed interface SyncMutation {
    data class StatusChanged(val status: SyncStatus) : SyncMutation
    data object ImportStarted : SyncMutation
    data class ImportFinished(val message: String?) : SyncMutation
}
