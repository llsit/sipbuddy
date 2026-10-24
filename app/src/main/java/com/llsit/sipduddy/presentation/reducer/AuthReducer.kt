package com.llsit.sipduddy.presentation.reducer

import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.presentation.model.*
import com.llsit.sipduddy.presentation.mutation.AuthMutation

internal class AuthReducer {
    fun reduce(state: AuthUiState, mutation: AuthMutation): AuthUiState = when (mutation) {
        AuthMutation.Opened -> state.copy(showForm = true, error = null, message = null)

        AuthMutation.Closed -> if (state.isBusy) state else state.copy(showForm = false)

        is AuthMutation.ModeChanged -> if (state.isBusy || state.mode == AuthMode.UPDATE_PASSWORD || mutation.mode == AuthMode.UPDATE_PASSWORD) {
            state
        } else {
            state.copy(mode = mutation.mode, error = null, message = null)
        }

        is AuthMutation.SessionChanged -> state.copy(
            session = mutation.session,
            showForm = if (mutation.session is AuthSession.SignedIn && state.mode != AuthMode.UPDATE_PASSWORD) false else state.showForm,
        )

        AuthMutation.RecoveryRequired -> state.copy(mode = AuthMode.UPDATE_PASSWORD, showForm = true, error = null, message = null)

        is AuthMutation.Failed -> state.copy(error = mutation.message, showForm = state.showForm || mutation.openForm)

        AuthMutation.Started -> state.copy(isBusy = true, error = null, message = null)

        AuthMutation.Finished -> state.copy(isBusy = false)

        is AuthMutation.MessageShown -> state.copy(message = mutation.message)

        AuthMutation.PasswordUpdated -> state.copy(showForm = false, mode = AuthMode.SIGN_IN, message = "เปลี่ยนรหัสผ่านแล้ว")

        AuthMutation.SignedOut -> state.copy(mode = AuthMode.SIGN_IN, message = "ออกจากระบบแล้ว ข้อมูลการดื่มยังอยู่ในเครื่อง")
    }
}
