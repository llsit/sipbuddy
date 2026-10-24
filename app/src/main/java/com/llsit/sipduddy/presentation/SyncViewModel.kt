package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llsit.sipduddy.domain.model.SyncStatus
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.domain.usecase.*
import com.llsit.sipduddy.presentation.intent.SyncIntent
import com.llsit.sipduddy.presentation.model.SyncUiState
import com.llsit.sipduddy.presentation.mutation.SyncMutation
import com.llsit.sipduddy.presentation.reducer.SyncReducer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SyncViewModel(
    observe: ObserveSyncStatusUseCase,
    private val request: RequestHydrationSyncUseCase,
    private val importGuestUseCase: ImportGuestHistoryUseCase,
    private val expectedOwner: String? = null,
    private val accounts: AccountRepository,
) : ViewModel() {
    private val reducer = SyncReducer()
    private val mutableState = MutableStateFlow(SyncUiState(status = SyncStatus(ownerId = expectedOwner.orEmpty())))
    val state = mutableState.asStateFlow()
    private fun isCurrentAccount() = expectedOwner == null || accounts.owner.value == expectedOwner
    private fun reduce(mutation: SyncMutation) {
        mutableState.update { reducer.reduce(it, mutation) }
    }
    init {
        viewModelScope.launch {
            observe().filter { isCurrentAccount() && (expectedOwner == null || it.ownerId == expectedOwner) }
                .collect { reduce(SyncMutation.StatusChanged(it)) }
        }
    }
    fun onIntent(intent: SyncIntent) {
        if (!isCurrentAccount()) return
        when (intent) {
            SyncIntent.SyncNow -> request()

            is SyncIntent.ImportGuestHistory -> {
                if (state.value.importing || intent.ownerId.isEmpty() || accounts.owner.value != intent.ownerId ||
                    (expectedOwner != null && intent.ownerId != expectedOwner)
                ) {
                    return
                }
                reduce(SyncMutation.ImportStarted)
                viewModelScope.launch {
                    var message: String? = null
                    try {
                        if (isCurrentAccount()) message = "ย้ายประวัติ ${importGuestUseCase(intent.ownerId)} รายการเข้าบัญชีแล้ว"
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        message = "ย้ายประวัติไม่สำเร็จ กรุณาลองใหม่"
                    } finally {
                        reduce(SyncMutation.ImportFinished(message))
                    }
                }
            }
        }
    }
}
