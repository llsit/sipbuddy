package com.llsit.sipduddy.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.llsit.sipduddy.R
import com.llsit.sipduddy.presentation.animation.SipBuddyMotion
import com.llsit.sipduddy.presentation.component.AppHeader
import com.llsit.sipduddy.presentation.component.DeleteDrinkDialog
import com.llsit.sipduddy.presentation.effect.HydrationEffect
import com.llsit.sipduddy.presentation.intent.HydrationIntent
import com.llsit.sipduddy.presentation.model.AppDestination
import com.llsit.sipduddy.presentation.screen.AddDrinkSheet
import com.llsit.sipduddy.presentation.screen.HistoryScreen
import com.llsit.sipduddy.presentation.screen.SettingsScreen
import com.llsit.sipduddy.presentation.screen.TodayScreen
import com.llsit.sipduddy.ui.theme.Cream
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipBuddyApp(viewModel: HydrationViewModel = koinViewModel(), accountContent: @Composable () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val destination = state.destination
    val focusManager = LocalFocusManager.current
    val screenStateHolder = rememberSaveableStateHolder()
    val deletingId = state.deletingId
    val snackbar = remember { SnackbarHostState() }
    val overview = state.overview
    val loadError = state.loadError
    val deleting = remember(overview, deletingId) {
        overview?.days?.asSequence()?.flatMap { it.entries.asSequence() }
            ?.firstOrNull { it.id == deletingId }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    HydrationEffect.DismissMessage -> snackbar.currentSnackbarData?.dismiss()

                    is HydrationEffect.ShowMessage -> {
                        val message = effect.message
                        val result = snackbar.showSnackbar(
                            message.text,
                            actionLabel = if (message.undoEntry != null) "เลิกทำ" else null,
                            withDismissAction = message.undoEntry != null,
                            duration = if (message.undoEntry != null) SnackbarDuration.Long else SnackbarDuration.Short,
                        )
                        viewModel.onIntent(
                            if (result == SnackbarResult.ActionPerformed && message.undoEntry != null) {
                                HydrationIntent.UndoDelete(message.id)
                            } else {
                                HydrationIntent.MessageHandled(message.id)
                            },
                        )
                    }
                }
            }
        }
    }
    Scaffold(
        topBar = @Composable { AppHeader() },
        floatingActionButton = @Composable {
            if (destination == AppDestination.TODAY && overview != null && loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = { if (!state.isSaving) viewModel.onIntent(HydrationIntent.OpenAddDrink) },
                    modifier = Modifier.semantics {
                        contentDescription = "บันทึกเครื่องดื่ม"
                        if (state.isSaving) disabled()
                    },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("บันทึกเครื่องดื่ม") },
                )
            }
        },
        snackbarHost = @Composable { SnackbarHost(hostState = snackbar) },
        bottomBar = @Composable {
            NavigationBar(containerColor = Cream, tonalElevation = 0.dp) {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onIntent(HydrationIntent.Navigate(item))
                        },
                        icon = @Composable { Icon(painterResource(item.iconRes), contentDescription = null) },
                        label = @Composable { Text(item.title) },
                    )
                }
            }
        },
        content = @Composable { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))

                    loadError != null -> {
                        Text(loadError)
                        TextButton(onClick = { viewModel.onIntent(HydrationIntent.Retry) }) { Text("ลองใหม่") }
                    }

                    overview != null -> AnimatedContent(
                        targetState = destination,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (
                                fadeIn(tween(SipBuddyMotion.CONTENT_MILLIS, delayMillis = SipBuddyMotion.EXIT_MILLIS)) togetherWith
                                    fadeOut(tween(SipBuddyMotion.EXIT_MILLIS))
                                ).using(null)
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
                                    onEdit = { viewModel.onIntent(HydrationIntent.OpenEditDrink(it)) },
                                    onDelete = { viewModel.onIntent(HydrationIntent.RequestDelete(it.id)) },
                                    waveEnabled = destination == AppDestination.TODAY && !state.isAddSheetVisible && deletingId == null && !transitionRunning,
                                )

                                AppDestination.HISTORY -> HistoryScreen(
                                    overview,
                                    state.isSaving,
                                    Modifier.fillMaxSize(),
                                    { viewModel.onIntent(HydrationIntent.OpenEditDrink(it)) },
                                ) { viewModel.onIntent(HydrationIntent.RequestDelete(it.id)) }

                                AppDestination.SETTINGS -> SettingsScreen(
                                    overview.settings,
                                    state.isSaving,
                                    Modifier.fillMaxSize(),
                                    { viewModel.onIntent(HydrationIntent.SaveSettings(it)) },
                                    accountContent = accountContent,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
    if (state.isAddSheetVisible) {
        val busy by rememberUpdatedState(state.isSaving)
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { value -> !busy || value != SheetValue.Hidden },
        )
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(HydrationIntent.DismissDrinkEditor) },
            sheetState = sheetState,
            containerColor = Cream,
        ) {
            AddDrinkSheet(state.isSaving, { drink, amount -> viewModel.onIntent(HydrationIntent.SaveDrink(drink, amount)) }, state.editingEntry)
        }
    }
    deleting?.let { entry ->
        DeleteDrinkDialog(
            entry,
            state.isSaving,
            onConfirm = {
                viewModel.onIntent(HydrationIntent.DeleteDrink(entry.id))
            },
            onDismiss = { viewModel.onIntent(HydrationIntent.DismissDelete) },
        )
    }
}
