package com.llsit.sipduddy.data.repository
import com.llsit.sipduddy.data.auth.SupabaseClientProvider
import com.llsit.sipduddy.data.mapper.AuthErrorMapper
import com.llsit.sipduddy.domain.model.AuthException
import kotlinx.coroutines.CancellationException
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.domain.model.AuthUser
import com.llsit.sipduddy.domain.repository.AuthRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SupabaseAuthRepository(private val provider: SupabaseClientProvider) : AuthRepository {
    private val auth get() = checkNotNull(provider.client) { "ระบบบัญชียังไม่พร้อมใช้งาน" }.auth
    override val isConfigured get() = provider.client != null
    override val recoveryRequired = provider.recoveryRequired
    override val callbackError = provider.callbackError
    override fun clearCallbackError() { provider.callbackError.value = null }
    private suspend fun <T> request(block: suspend () -> T): T = try { block() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { throw AuthException(AuthErrorMapper().format(error)) }
    override val session: Flow<AuthSession> = provider.client?.auth?.sessionStatus?.map { status ->
        when (status) {
            SessionStatus.Initializing -> AuthSession.Loading
            is SessionStatus.Authenticated -> status.session.user?.let { AuthSession.SignedIn(AuthUser(it.id, it.email)) }
                ?: AuthSession.SignedOut
            else -> AuthSession.SignedOut
        }
    } ?: flowOf(AuthSession.SignedOut)
    override suspend fun signIn(email: String, password: String) = request {
        auth.awaitInitialization()
        auth.signInWith(Email) { this.email = email; this.password = password }
        Unit
    }
    override suspend fun signUp(email: String, password: String): Boolean = request {
        auth.awaitInitialization()
        auth.signUpWith(Email, redirectUrl = provider.redirectUrl) { this.email = email; this.password = password }
        auth.currentSessionOrNull() != null
    }
    override suspend fun signInWithGoogle() = request {
        auth.awaitInitialization()
        auth.signInWith(Google, redirectUrl = provider.redirectUrl)
        Unit
    }
    override suspend fun signOut() = request {
        auth.signOut()
        provider.recoveryRequired.value = false
    }
    override suspend fun sendPasswordReset(email: String) = request {
        auth.awaitInitialization()
        auth.resetPasswordForEmail(email, redirectUrl = "${provider.redirectUrl}?mode=recovery")
        Unit
    }
    override suspend fun updatePassword(password: String) = request {
        check(provider.recoveryRequired.value) { "กรุณาเปิดลิงก์รีเซ็ตรหัสผ่านก่อน" }
        auth.updateUser { this.password = password }
        provider.recoveryRequired.value = false
    }
}
