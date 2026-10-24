package com.llsit.sipduddy.presentation.mutation

import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.presentation.model.AuthMode

internal sealed interface AuthMutation {
    data object Opened : AuthMutation
    data object Closed : AuthMutation
    data class ModeChanged(val mode: AuthMode) : AuthMutation
    data class SessionChanged(val session: AuthSession) : AuthMutation
    data object RecoveryRequired : AuthMutation
    data class Failed(val message: String, val openForm: Boolean = false) : AuthMutation
    data object Started : AuthMutation
    data object Finished : AuthMutation
    data class MessageShown(val message: String) : AuthMutation
    data object PasswordUpdated : AuthMutation
    data object SignedOut : AuthMutation
}
