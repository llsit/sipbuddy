package com.llsit.sipduddy.presentation.reducer

import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.presentation.model.*
import com.llsit.sipduddy.presentation.mutation.HydrationMutation
import org.junit.Assert.*
import org.junit.Test

class HydrationReducerTest {
    private val reducer = HydrationReducer()

    @Test fun staleAcknowledgementCannotDismissNewUndo() {
        val entry = DrinkEntry(1, DrinkType.WATER, 250, 100)
        val pending = UiMessage(2, "deleted", entry)
        val initial = HydrationUiState(message = pending)
        assertEquals(initial, reducer.reduce(initial, HydrationMutation.MessageHandled(1)))
        assertNull(reducer.reduce(initial, HydrationMutation.MessageHandled(2)).message)
        assertEquals(pending, initial.message)
    }

    @Test fun editorCannotChangeDuringWriteAndClosingClearsSnapshot() {
        val entry = DrinkEntry(1, DrinkType.WATER, 250, 100)
        val initial = HydrationUiState(isSaving = true)
        assertEquals(initial, reducer.reduce(initial, HydrationMutation.EditorOpened(entry)))
        val opened = reducer.reduce(initial.copy(isSaving = false), HydrationMutation.EditorOpened(entry))
        assertTrue(opened.isAddSheetVisible)
        assertEquals(entry, opened.editingEntry)
        val closed = reducer.reduce(opened, HydrationMutation.EditorClosed)
        assertFalse(closed.isAddSheetVisible)
        assertNull(closed.editingEntry)
    }

    @Test fun navigationAndDeleteConfirmationPreserveOtherState() {
        val initial = HydrationUiState(isSaving = true, message = UiMessage(4, "saved"))
        val navigated = reducer.reduce(initial, HydrationMutation.Navigated(AppDestination.HISTORY))
        val requested = reducer.reduce(navigated, HydrationMutation.DeleteRequested(8))
        assertEquals(AppDestination.HISTORY, requested.destination)
        assertEquals(8L, requested.deletingId)
        assertTrue(requested.isSaving)
        assertEquals(initial.message, requested.message)
        assertNull(reducer.reduce(requested, HydrationMutation.DeleteRequested(null)).deletingId)
    }

    @Test fun failedLoadAndRetryHaveDeterministicStates() {
        val failed = reducer.reduce(HydrationUiState(), HydrationMutation.LoadFailed)
        assertFalse(failed.loading)
        assertNotNull(failed.loadError)
        val retry = reducer.reduce(failed, HydrationMutation.RetryStarted)
        assertTrue(retry.loading)
        assertNull(retry.loadError)
    }
}
