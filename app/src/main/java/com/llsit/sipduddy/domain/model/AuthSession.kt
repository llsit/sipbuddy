package com.llsit.sipduddy.domain.model
sealed interface AuthSession {
    data object Loading : AuthSession
    data object SignedOut : AuthSession
    data class SignedIn(val user: AuthUser, val isOffline: Boolean = false) : AuthSession
}
