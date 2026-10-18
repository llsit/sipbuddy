package com.llsit.sipduddy.domain.repository
import com.llsit.sipduddy.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isConfigured: Boolean
    val session: Flow<AuthSession>
    val recoveryRequired: Flow<Boolean>
    val callbackError: Flow<String?>
    fun clearCallbackError()
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String): Boolean
    suspend fun signInWithGoogle()
    suspend fun signOut()
    suspend fun sendPasswordReset(email: String)
    suspend fun updatePassword(password: String)
}
