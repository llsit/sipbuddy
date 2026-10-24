package com.llsit.sipduddy.presentation.reducer

import com.llsit.sipduddy.domain.model.SyncStatus
import com.llsit.sipduddy.presentation.model.SyncUiState
import com.llsit.sipduddy.presentation.mutation.SyncMutation
import org.junit.Assert.*
import org.junit.Test

class SyncReducerTest {
    @Test fun incomingStatusPreservesImportAndCompletionPreservesStatus() {
        val reducer = SyncReducer()
        val started = reducer.reduce(SyncUiState(message = "old"), SyncMutation.ImportStarted)
        val status = SyncStatus(ownerId = "A", pending = 3)
        val updated = reducer.reduce(started, SyncMutation.StatusChanged(status))
        assertTrue(updated.importing)
        assertNull(updated.message)
        val finished = reducer.reduce(updated, SyncMutation.ImportFinished("imported"))
        assertFalse(finished.importing)
        assertEquals(status, finished.status)
        assertEquals("imported", finished.message)
    }
}
