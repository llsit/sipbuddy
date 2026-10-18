package com.llsit.sipduddy.fake
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.domain.model.AuthUser
import com.llsit.sipduddy.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    override var isConfigured = true
    override val session = MutableStateFlow<AuthSession>(AuthSession.SignedOut)
    override val recoveryRequired = MutableStateFlow(false)
    override val callbackError = MutableStateFlow<String?>(null)
    override fun clearCallbackError() {
        callbackError.value = null
    }
    var calls = 0
    var googleCalls = 0
    var passwordUpdates = 0
    var lastEmail: String? = null
    var lastPassword: String? = null
    var failure: Exception? = null
    var gate: CompletableDeferred<Unit>? = null
    var signupHasSession = false
    private suspend fun request(email: String? = null, password: String? = null) {
        calls++
        lastEmail = email
        lastPassword = password
        gate?.await()
        failure?.let { throw it }
    }
    override suspend fun signIn(email: String, password: String) {
        request(email, password)
        session.value = AuthSession.SignedIn(AuthUser("user-1", email))
    }
    override suspend fun signUp(email: String, password: String): Boolean {
        request(email, password)
        if (signupHasSession) session.value = AuthSession.SignedIn(AuthUser("user-1", email))
        return signupHasSession
    }
    override suspend fun signInWithGoogle() {
        googleCalls++
        request()
    }
    override suspend fun signOut() {
        request()
        session.value = AuthSession.SignedOut
    }
    override suspend fun sendPasswordReset(email: String) {
        request(email)
    }
    override suspend fun updatePassword(password: String) {
        request(password = password)
        passwordUpdates++
        recoveryRequired.value = false
    }
}
