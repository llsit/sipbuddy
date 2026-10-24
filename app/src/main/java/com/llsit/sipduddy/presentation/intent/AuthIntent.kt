package com.llsit.sipduddy.presentation.intent

import com.llsit.sipduddy.presentation.model.AuthMode

sealed interface AuthIntent {
    data object Open : AuthIntent
    data object Close : AuthIntent
    data class ChangeMode(val mode: AuthMode) : AuthIntent

    // Credentials are handled immediately and never stored in UiState or logged.
    class Submit(val email: String, val password: String) : AuthIntent
    data object GoogleSignIn : AuthIntent
    data object SignOut : AuthIntent
}
