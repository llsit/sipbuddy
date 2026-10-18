package com.llsit.sipduddy.presentation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llsit.sipduddy.presentation.component.AccountCard
import com.llsit.sipduddy.presentation.screen.AuthScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SipBuddyRoot(authViewModel: AuthViewModel = koinViewModel()) {
    val state by authViewModel.state.collectAsStateWithLifecycle()
    SipBuddyApp(accountContent = { AccountCard(state, authViewModel::open, authViewModel::signOut) })
    if (state.showForm) Dialog(onDismissRequest = authViewModel::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !state.isBusy)) {
        Surface { AuthScreen(state, authViewModel::mode, authViewModel::submit, authViewModel::google, authViewModel::close) }
    }
}
