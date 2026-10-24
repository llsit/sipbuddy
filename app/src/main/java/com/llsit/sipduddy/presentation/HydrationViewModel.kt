package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.SaveHydrationSettingsResult
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.domain.usecase.AddDrinkUseCase
import com.llsit.sipduddy.domain.usecase.DeleteDrinkUseCase
import com.llsit.sipduddy.domain.usecase.EditDrinkUseCase
import com.llsit.sipduddy.domain.usecase.ObserveHydrationOverviewUseCase
import com.llsit.sipduddy.domain.usecase.RestoreDrinkUseCase
import com.llsit.sipduddy.domain.usecase.SaveHydrationSettingsUseCase
import com.llsit.sipduddy.presentation.effect.HydrationEffect
import com.llsit.sipduddy.presentation.intent.HydrationIntent
import com.llsit.sipduddy.presentation.model.HydrationUiState
import com.llsit.sipduddy.presentation.model.UiMessage
import com.llsit.sipduddy.presentation.mutation.HydrationMutation
import com.llsit.sipduddy.presentation.reducer.HydrationReducer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class HydrationViewModel(
    observeHydrationOverviewUseCase: ObserveHydrationOverviewUseCase,
    private val addDrinkUseCase: AddDrinkUseCase,
    private val deleteDrinkUseCase: DeleteDrinkUseCase,
    private val saveHydrationSettingsUseCase: SaveHydrationSettingsUseCase,
    private val editDrinkUseCase: EditDrinkUseCase,
    private val restoreDrinkUseCase: RestoreDrinkUseCase,
    private val accounts: AccountRepository? = null,
    expectedOwner: String? = accounts?.owner?.value,
) : ViewModel() {
    private val boundOwner = expectedOwner
    private fun isCurrentAccount() = accounts == null || accounts.owner.value == boundOwner
    private val retryVersion = MutableStateFlow(0)
    private val reducer = HydrationReducer()
    private val mutableState = MutableStateFlow(HydrationUiState())
    val state = mutableState.asStateFlow()

    // Pending UI effects live in state until acknowledged, so backgrounding cannot lose Undo.
    val effects: Flow<HydrationEffect> = state.map { current ->
        current.message?.let { HydrationEffect.ShowMessage(it) } ?: HydrationEffect.DismissMessage
    }.distinctUntilChanged()

    private fun reduce(mutation: HydrationMutation) {
        mutableState.update { reducer.reduce(it, mutation) }
    }

    private val messageId = AtomicLong()
    private val actionMutex = Mutex()

    init {
        viewModelScope.launch {
            retryVersion.collectLatest {
                observeHydrationOverviewUseCase()
                    .filter { overview ->
                        isCurrentAccount() && (
                            boundOwner == null ||
                                (overview.settings.ownerId == boundOwner && overview.days.all { day -> day.entries.all { entry -> entry.ownerId == boundOwner } })
                            )
                    }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        reduce(HydrationMutation.LoadFailed)
                    }
                    .collect { reduce(HydrationMutation.Loaded(it)) }
            }
        }
    }

    fun onIntent(intent: HydrationIntent) {
        if (!isCurrentAccount()) return
        when (intent) {
            HydrationIntent.Retry -> retry()

            HydrationIntent.OpenAddDrink -> showAddDrinkSheet()

            is HydrationIntent.OpenEditDrink -> {
                if (boundOwner == null || intent.entry.ownerId == boundOwner) showEditDrinkSheet(intent.entry)
            }

            HydrationIntent.DismissDrinkEditor -> dismissAddDrinkSheet()

            is HydrationIntent.SaveDrink -> saveDrink(intent.drink, intent.amountMl)

            is HydrationIntent.DeleteDrink -> {
                reduce(HydrationMutation.DeleteRequested(null))
                delete(intent.id)
            }

            is HydrationIntent.UndoDelete -> undoDelete(intent.messageId)

            is HydrationIntent.SaveSettings -> save(intent.settings)

            is HydrationIntent.MessageHandled -> clearMessage(intent.id)

            is HydrationIntent.Navigate -> reduce(HydrationMutation.Navigated(intent.destination))

            is HydrationIntent.RequestDelete -> if (!state.value.isSaving) reduce(HydrationMutation.DeleteRequested(intent.id))

            HydrationIntent.DismissDelete -> reduce(HydrationMutation.DeleteRequested(null))
        }
    }

    private fun retry() {
        reduce(HydrationMutation.RetryStarted)
        retryVersion.update { it + 1 }
    }

    private fun showAddDrinkSheet() {
        reduce(HydrationMutation.EditorOpened(null))
    }

    private fun showEditDrinkSheet(entry: DrinkEntry) {
        reduce(HydrationMutation.EditorOpened(entry))
    }

    private fun saveDrink(drink: DrinkType, amountMl: Int) {
        val editing = state.value.editingEntry
        if (editing == null) {
            add(drink, amountMl)
        } else {
            launchAction {
                editDrinkUseCase(editing.id, drink, amountMl)
                reduce(HydrationMutation.EditorClosed)
                showMessage("แก้ไขรายการแล้ว")
            }
        }
    }

    private fun dismissAddDrinkSheet() {
        if (!state.value.isSaving) reduce(HydrationMutation.EditorClosed)
    }

    private fun add(drink: DrinkType, amountMl: Int) = launchAction {
        addDrinkUseCase(drink, amountMl, boundOwner)
        reduce(HydrationMutation.EditorClosed)
        showMessage("บันทึกเครื่องดื่มแล้ว")
    }

    private fun delete(id: Long) = launchAction {
        val deleted = deleteDrinkUseCase(id)
        if (deleted != null) reduce(HydrationMutation.MessageShown(UiMessage(messageId.incrementAndGet(), "ลบรายการแล้ว", deleted)))
    }

    private fun undoDelete(id: Long) {
        if (!isCurrentAccount()) return
        val pending = state.value.message?.takeIf { it.id == id && it.undoEntry != null } ?: return
        if (state.value.message != pending) return
        reduce(HydrationMutation.MessageHandled(pending.id))
        viewModelScope.launch {
            // Queue a pressed Undo rather than dropping it while another write holds the mutex.
            actionMutex.withLock {
                if (!isCurrentAccount()) return@withLock
                reduce(HydrationMutation.Saving(true))
                try {
                    val restored = restoreDrinkUseCase(requireNotNull(pending.undoEntry))
                    showMessage(if (restored) "คืนรายการแล้ว" else "รายการนี้มีอยู่แล้ว")
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    reduce(HydrationMutation.MessageShown(pending.copy(id = messageId.incrementAndGet(), text = "คืนรายการไม่สำเร็จ ลองอีกครั้ง")))
                } finally {
                    reduce(HydrationMutation.Saving(false))
                }
            }
        }
    }

    private fun save(settings: HydrationSettings) = launchAction {
        val result = saveHydrationSettingsUseCase(settings, boundOwner)
        showMessage(
            if (result == SaveHydrationSettingsResult.SAVED) {
                "บันทึกการตั้งค่าแล้ว"
            } else {
                "บันทึกการตั้งค่าแล้ว แต่ตั้งเวลาเตือนไม่สำเร็จ กรุณาลองบันทึกอีกครั้ง"
            },
        )
    }

    private fun clearMessage(id: Long) {
        reduce(HydrationMutation.MessageHandled(id))
    }

    private fun showMessage(text: String) {
        reduce(HydrationMutation.MessageShown(UiMessage(messageId.incrementAndGet(), text)))
    }

    private fun launchAction(action: suspend () -> Unit) {
        if (!isCurrentAccount() || !actionMutex.tryLock()) return
        reduce(HydrationMutation.Saving(true))
        viewModelScope.launch {
            try {
                if (isCurrentAccount()) action()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                showMessage(exception.message ?: "ข้อมูลไม่ถูกต้อง")
            } catch (_: Exception) {
                showMessage("บันทึกไม่สำเร็จ กรุณาลองอีกครั้ง")
            } finally {
                reduce(HydrationMutation.Saving(false))
                actionMutex.unlock()
            }
        }
    }
}
