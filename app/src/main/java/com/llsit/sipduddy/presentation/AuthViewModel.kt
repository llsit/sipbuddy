package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.domain.usecase.*
import com.llsit.sipduddy.presentation.formatter.AuthErrorFormatter
import com.llsit.sipduddy.presentation.intent.AuthIntent
import com.llsit.sipduddy.presentation.model.*
import com.llsit.sipduddy.presentation.mutation.AuthMutation
import com.llsit.sipduddy.presentation.reducer.AuthReducer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
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
    private val reducer = AuthReducer()
    private val mutableState = MutableStateFlow(AuthUiState(isConfigured = observeSession.isConfigured))
    val state = mutableState.asStateFlow()
    private fun reduce(mutation: AuthMutation) {
        mutableState.update { reducer.reduce(it, mutation) }
    }

    init {
        viewModelScope.launch {
            observeSession.callbackError().collect { error ->
                if (error != null) {
                    reduce(AuthMutation.Failed(error, openForm = true))
                    observeSession.clearCallbackError()
                }
            }
        }
        viewModelScope.launch { observeSession().collect { reduce(AuthMutation.SessionChanged(it)) } }
        viewModelScope.launch {
            observeSession.recoveryRequired().collect { if (it) reduce(AuthMutation.RecoveryRequired) }
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            AuthIntent.Open -> reduce(AuthMutation.Opened)

            AuthIntent.Close -> reduce(AuthMutation.Closed)

            is AuthIntent.ChangeMode -> reduce(AuthMutation.ModeChanged(intent.mode))

            is AuthIntent.Submit -> {
                val mode = state.value.mode
                perform {
                    when (mode) {
                        AuthMode.SIGN_IN -> signInUseCase(intent.email, intent.password)

                        AuthMode.SIGN_UP -> {
                            val active = signUpUseCase(intent.email, intent.password)
                            reduce(AuthMutation.MessageShown(if (active) "สมัครสมาชิกแล้ว" else "หากอีเมลนี้สมัครได้ ระบบจะส่งอีเมลยืนยัน กรุณาตรวจกล่องจดหมายและสแปม"))
                        }

                        AuthMode.RESET -> {
                            sendPasswordResetUseCase(intent.email)
                            reduce(AuthMutation.MessageShown("หากอีเมลนี้มีบัญชีอยู่ ระบบจะส่งลิงก์รีเซ็ตรหัสผ่าน กรุณาเปิดบนเครื่องนี้"))
                        }

                        AuthMode.UPDATE_PASSWORD -> {
                            updatePasswordUseCase(intent.password)
                            reduce(AuthMutation.PasswordUpdated)
                        }
                    }
                }
            }

            AuthIntent.GoogleSignIn -> perform { signInWithGoogleUseCase() }

            AuthIntent.SignOut -> perform {
                signOutUseCase()
                reduce(AuthMutation.SignedOut)
            }
        }
    }

    private fun perform(action: suspend () -> Unit) {
        if (state.value.isBusy || !state.value.isConfigured || state.value.session == AuthSession.Loading) return
        reduce(AuthMutation.Started)
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reduce(AuthMutation.Failed(errorFormatter.format(error)))
            } finally {
                reduce(AuthMutation.Finished)
            }
        }
    }
}
