package com.llsit.sipduddy.presentation.reducer

import com.llsit.sipduddy.presentation.model.HydrationUiState
import com.llsit.sipduddy.presentation.mutation.HydrationMutation

internal class HydrationReducer {
    fun reduce(state: HydrationUiState, mutation: HydrationMutation): HydrationUiState = when (mutation) {
        HydrationMutation.RetryStarted -> state.copy(loading = state.overview == null, loadError = null)
        is HydrationMutation.Loaded -> state.copy(loading = false, overview = mutation.overview, loadError = null)
        HydrationMutation.LoadFailed -> state.copy(loading = false, loadError = "อ่านข้อมูลไม่สำเร็จ กรุณาลองใหม่")
        is HydrationMutation.Saving -> state.copy(isSaving = mutation.value)
        is HydrationMutation.EditorOpened -> if (state.isSaving) state else state.copy(isAddSheetVisible = true, editingEntry = mutation.entry)
        HydrationMutation.EditorClosed -> state.copy(isAddSheetVisible = false, editingEntry = null)
        is HydrationMutation.MessageShown -> state.copy(message = mutation.message)
        is HydrationMutation.MessageHandled -> if (state.message?.id == mutation.id) state.copy(message = null) else state
        is HydrationMutation.Navigated -> state.copy(destination = mutation.destination)
        is HydrationMutation.DeleteRequested -> state.copy(deletingId = mutation.id)
    }
}
