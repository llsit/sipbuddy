package com.llsit.sipduddy.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.llsit.sipduddy.presentation.animation.SipBuddyMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.llsit.sipduddy.R
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llsit.sipduddy.presentation.component.AppHeader
import com.llsit.sipduddy.presentation.component.DeleteDrinkDialog
import com.llsit.sipduddy.presentation.model.AppDestination
import com.llsit.sipduddy.presentation.screen.AddDrinkSheet
import com.llsit.sipduddy.presentation.screen.HistoryScreen
import com.llsit.sipduddy.presentation.screen.SettingsScreen
import com.llsit.sipduddy.presentation.screen.TodayScreen
import com.llsit.sipduddy.ui.theme.Cream
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipBuddyApp(viewModel: HydrationViewModel = koinViewModel(), accountContent: @Composable () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(AppDestination.TODAY) }
    val focusManager = LocalFocusManager.current
    val screenStateHolder = rememberSaveableStateHolder()
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val overview = state.overview
    val loadError = state.loadError
    val message = state.message
    val deleting = remember(overview, deletingId) {
        overview?.days?.asSequence()?.flatMap { it.entries.asSequence() }
            ?.firstOrNull { it.id == deletingId }
    }
    LaunchedEffect(message?.id) {
        message?.let {
            val result = snackbar.showSnackbar(
                it.text,
                actionLabel = if (it.undoEntry != null) "เลิกทำ" else null,
                withDismissAction = it.undoEntry != null,
                duration = if (it.undoEntry != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && it.undoEntry != null) viewModel.undoDelete(it.id)
            else viewModel.clearMessage(it.id)
        }
    }
    Scaffold(
        topBar = @Composable { AppHeader() },
        floatingActionButton = @Composable {
            if (destination == AppDestination.TODAY && overview != null && loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = { if (!state.isSaving) viewModel.showAddDrinkSheet() },
                    modifier = Modifier.semantics {
                        contentDescription = "บันทึกเครื่องดื่ม"
                        if (state.isSaving) disabled()
                    },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("บันทึกเครื่องดื่ม") },
                )
            }
        },
        snackbarHost = @Composable { SnackbarHost(hostState = snackbar) }, bottomBar = @Composable {
        NavigationBar(containerColor = Cream, tonalElevation = 0.dp) {
            AppDestination.entries.forEach { item ->
                NavigationBarItem(
                    selected = destination == item, onClick = { focusManager.clearFocus(); destination = item },
                    icon = @Composable { Icon(painterResource(item.iconRes), contentDescription = null) },
                    label = @Composable { Text(item.title) })
            }
        }
    }, content = @Composable { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                loadError != null -> {
                    Text(loadError)
                    TextButton(onClick = viewModel::retry) { Text("ลองใหม่") }
                }

                overview != null -> AnimatedContent(
                    targetState = destination,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (fadeIn(tween(SipBuddyMotion.CONTENT_MILLIS, delayMillis = SipBuddyMotion.EXIT_MILLIS)) togetherWith
                            fadeOut(tween(SipBuddyMotion.EXIT_MILLIS))).using(null)
                    },
                    label = "destination",
                ) { shownDestination ->
                    val transitionRunning = transition.isRunning
                    screenStateHolder.SaveableStateProvider(shownDestination.name) {
                    when (shownDestination) {
                        AppDestination.TODAY -> TodayScreen(
                            overview,
                            state.isSaving,
                            Modifier.fillMaxSize(),
                            onEdit = viewModel::showEditDrinkSheet,
                            onDelete = { deletingId = it.id },
                            waveEnabled = destination == AppDestination.TODAY && !state.isAddSheetVisible && deletingId == null && !transitionRunning,
                        )

                        AppDestination.HISTORY -> HistoryScreen(
                            overview,
                            state.isSaving,
                            Modifier.fillMaxSize(),
                            viewModel::showEditDrinkSheet
                        ) { deletingId = it.id }

                        AppDestination.SETTINGS -> SettingsScreen(
                            overview.settings,
                            state.isSaving,
                            Modifier.fillMaxSize(),
                            viewModel::save,
                            accountContent = accountContent
                        )
                    }
                    }
                }
            }
        }
    })
    if (state.isAddSheetVisible) {
        val busy by rememberUpdatedState(state.isSaving)
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { value -> !busy || value != SheetValue.Hidden },
        )
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissAddDrinkSheet,
            sheetState = sheetState,
            containerColor = Cream
        ) {
            AddDrinkSheet(state.isSaving, viewModel::saveDrink, state.editingEntry)
        }
    }
    deleting?.let { entry ->
        DeleteDrinkDialog(
            entry,
            state.isSaving,
            onConfirm = { viewModel.delete(entry.id); deletingId = null },
            onDismiss = { deletingId = null })
    }
}
