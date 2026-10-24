package com.llsit.sipduddy.presentation.reducer

import com.llsit.sipduddy.presentation.model.SyncUiState
import com.llsit.sipduddy.presentation.mutation.SyncMutation

internal class SyncReducer {
    fun reduce(state: SyncUiState, mutation: SyncMutation): SyncUiState = when (mutation) {
        is SyncMutation.StatusChanged -> state.copy(status = mutation.status)
        SyncMutation.ImportStarted -> state.copy(importing = true, message = null)
        is SyncMutation.ImportFinished -> state.copy(importing = false, message = mutation.message)
    }
}
