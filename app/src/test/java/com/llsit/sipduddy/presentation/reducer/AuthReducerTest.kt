package com.llsit.sipduddy.presentation.reducer

import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.presentation.model.*
import com.llsit.sipduddy.presentation.mutation.AuthMutation
import org.junit.Assert.*
import org.junit.Test

class AuthReducerTest {
    private val reducer = AuthReducer()

    @Test fun busyStateRejectsCloseAndModeSwitch() {
        val initial = AuthUiState(isBusy = true, showForm = true)
        assertEquals(initial, reducer.reduce(initial, AuthMutation.Closed))
        assertEquals(initial, reducer.reduce(initial, AuthMutation.ModeChanged(AuthMode.SIGN_UP)))
    }

    @Test fun onlyRecoveryCanEnterPasswordUpdateAndUserCannotLeaveItByChangingMode() {
        val initial = AuthUiState()
        assertEquals(initial, reducer.reduce(initial, AuthMutation.ModeChanged(AuthMode.UPDATE_PASSWORD)))
        val recovery = reducer.reduce(initial, AuthMutation.RecoveryRequired)
        assertTrue(recovery.showForm)
        assertEquals(AuthMode.UPDATE_PASSWORD, recovery.mode)
        assertEquals(recovery, reducer.reduce(recovery, AuthMutation.ModeChanged(AuthMode.SIGN_IN)))
        val finished = reducer.reduce(recovery, AuthMutation.PasswordUpdated)
        assertFalse(finished.showForm)
        assertEquals(AuthMode.SIGN_IN, finished.mode)
    }

    @Test fun operationStartClearsOldFeedbackAndFinishPreservesFailure() {
        val started = reducer.reduce(AuthUiState(error = "old", message = "old"), AuthMutation.Started)
        assertTrue(started.isBusy)
        assertNull(started.error)
        assertNull(started.message)
        val failed = reducer.reduce(started, AuthMutation.Failed("retry", openForm = true))
        val finished = reducer.reduce(failed, AuthMutation.Finished)
        assertFalse(finished.isBusy)
        assertTrue(finished.showForm)
        assertEquals("retry", finished.error)
    }
}
