package com.llsit.sipduddy.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.domain.usecase.*
import com.llsit.sipduddy.presentation.formatter.AuthErrorFormatter
import com.llsit.sipduddy.presentation.model.AuthMode
import com.llsit.sipduddy.presentation.model.AuthUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    observeSession: ObserveAuthSessionUseCase,
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val updatePasswordUseCase: UpdatePasswordUseCase,
    private val errorFormatter: AuthErrorFormatter,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState(isConfigured = observeSession.isConfigured))
    val state = mutableState.asStateFlow()
    init {
        viewModelScope.launch {
            observeSession.callbackError().collect { error ->
                if (error != null) {
                    mutableState.update { it.copy(error = error, showForm = true) }
                    observeSession.clearCallbackError()
                }
            }
        }
        viewModelScope.launch {
            observeSession().collect { session -> mutableState.update {
                it.copy(session = session, showForm = if (session is AuthSession.SignedIn && it.mode != AuthMode.UPDATE_PASSWORD) false else it.showForm)
            } }
        }
        viewModelScope.launch {
            observeSession.recoveryRequired().collect { required ->
                if (required) mutableState.update { it.copy(mode = AuthMode.UPDATE_PASSWORD, showForm = true, error = null, message = null) }
            }
        }
    }
    fun open() { mutableState.update { it.copy(showForm = true, error = null, message = null) } }
    fun close() { if (!state.value.isBusy) mutableState.update { it.copy(showForm = false) } }
    fun mode(mode: AuthMode) {
        if (!state.value.isBusy && state.value.mode != AuthMode.UPDATE_PASSWORD && mode != AuthMode.UPDATE_PASSWORD)
            mutableState.update { it.copy(mode = mode, error = null, message = null) }
    }
    fun submit(email: String, password: String) = perform {
        when (state.value.mode) {
            AuthMode.SIGN_IN -> signInUseCase(email, password)
            AuthMode.SIGN_UP -> {
                val active = signUpUseCase(email, password)
                mutableState.update { it.copy(message = if (active) "สมัครสมาชิกแล้ว" else "หากอีเมลนี้สมัครได้ ระบบจะส่งอีเมลยืนยัน กรุณาตรวจกล่องจดหมายและสแปม") }
            }
            AuthMode.RESET -> {
                sendPasswordResetUseCase(email)
                mutableState.update { it.copy(message = "หากอีเมลนี้มีบัญชีอยู่ ระบบจะส่งลิงก์รีเซ็ตรหัสผ่าน กรุณาเปิดบนเครื่องนี้") }
            }
            AuthMode.UPDATE_PASSWORD -> {
                updatePasswordUseCase(password)
                mutableState.update { it.copy(showForm = false, mode = AuthMode.SIGN_IN, message = "เปลี่ยนรหัสผ่านแล้ว") }
            }
        }
    }
    fun google() = perform { signInWithGoogleUseCase() }
    fun signOut() = perform {
        signOutUseCase()
        mutableState.update { it.copy(mode = AuthMode.SIGN_IN, message = "ออกจากระบบแล้ว ข้อมูลการดื่มยังอยู่ในเครื่อง") }
    }
    private fun perform(action: suspend () -> Unit) {
        if (state.value.isBusy || !state.value.isConfigured || state.value.session == AuthSession.Loading) return
        mutableState.update { it.copy(isBusy = true, error = null, message = null) }
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(error = errorFormatter.format(error)) } }
            finally { mutableState.update { it.copy(isBusy = false) } }
        }
    }
}
