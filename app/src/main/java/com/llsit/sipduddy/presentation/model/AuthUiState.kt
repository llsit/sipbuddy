package com.llsit.sipduddy.presentation.model
import com.llsit.sipduddy.domain.model.AuthSession
data class AuthUiState(
    val session: AuthSession = AuthSession.Loading,
    val isConfigured: Boolean = false,
    val isBusy: Boolean = false,
    val showForm: Boolean = false,
    val mode: AuthMode = AuthMode.SIGN_IN,
    val error: String? = null,
    val message: String? = null,
)
