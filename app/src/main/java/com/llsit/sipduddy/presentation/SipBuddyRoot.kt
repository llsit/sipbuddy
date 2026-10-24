package com.llsit.sipduddy.presentation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.presentation.component.AccountCard
import com.llsit.sipduddy.presentation.component.SyncCard
import com.llsit.sipduddy.presentation.intent.AuthIntent
import com.llsit.sipduddy.presentation.intent.SyncIntent
import com.llsit.sipduddy.presentation.screen.AuthScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun SipBuddyRoot(authViewModel: AuthViewModel = koinViewModel()) {
    val auth by authViewModel.state.collectAsStateWithLifecycle()
    val accounts = koinInject<AccountRepository>()
    val owner by accounts.owner.collectAsStateWithLifecycle()
    val accountScope: AccountScopeViewModel = koinViewModel()
    val accountStore = accountScope.storeFor(owner)
    val authOwner = when (val session = auth.session) {
        AuthSession.Loading -> null
        AuthSession.SignedOut -> ""
        is AuthSession.SignedIn -> session.user.id
    }
    // Auth and Room account streams may be delivered on different dispatchers.
    // Never display a diary under a different account's header, even during that transition.
    if (owner == null || owner != authOwner) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        key(owner) {
            // Retain state across rotation; the account scope clears every store when the owner changes.
            val store = accountStore
            val storeOwner = remember(store) {
                object : ViewModelStoreOwner {
                    override val viewModelStore = store
                }
            }
            CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
                val syncViewModel: SyncViewModel = koinViewModel(parameters = { parametersOf(owner) })
                val hydrationViewModel: HydrationViewModel = koinViewModel(parameters = { parametersOf(owner) })
                val sync by syncViewModel.state.collectAsStateWithLifecycle()
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { syncViewModel.onIntent(SyncIntent.SyncNow) }
                SipBuddyApp(viewModel = hydrationViewModel, accountContent = {
                    AccountCard(auth, { authViewModel.onIntent(AuthIntent.Open) }, { authViewModel.onIntent(AuthIntent.SignOut) })
                    SyncCard(sync, { syncViewModel.onIntent(SyncIntent.SyncNow) }, { syncViewModel.onIntent(SyncIntent.ImportGuestHistory(it)) })
                })
            }
        }
    }
    if (auth.showForm) {
        Dialog(
            onDismissRequest = { authViewModel.onIntent(AuthIntent.Close) },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !auth.isBusy),
        ) {
            Surface { AuthScreen(auth, authViewModel::onIntent) }
        }
    }
}
