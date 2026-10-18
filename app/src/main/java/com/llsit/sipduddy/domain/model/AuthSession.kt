package com.llsit.sipduddy.domain.model
sealed interface AuthSession {
    data object Loading : AuthSession
    data object SignedOut : AuthSession
    data class SignedIn(val user: AuthUser) : AuthSession
}
