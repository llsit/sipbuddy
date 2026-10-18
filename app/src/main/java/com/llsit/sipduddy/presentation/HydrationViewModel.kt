package com.llsit.sipduddy.presentation

import com.llsit.sipduddy.domain.repository.AccountRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.usecase.EditDrinkUseCase
import com.llsit.sipduddy.domain.usecase.RestoreDrinkUseCase
import kotlinx.coroutines.sync.withLock
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.SaveHydrationSettingsResult
import com.llsit.sipduddy.domain.usecase.AddDrinkUseCase
import com.llsit.sipduddy.domain.usecase.DeleteDrinkUseCase
import com.llsit.sipduddy.domain.usecase.ObserveHydrationOverviewUseCase
import com.llsit.sipduddy.domain.usecase.SaveHydrationSettingsUseCase
import com.llsit.sipduddy.presentation.model.HydrationUiState
import com.llsit.sipduddy.presentation.model.OverviewLoadState
import com.llsit.sipduddy.presentation.model.UiMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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
    private val isSaving = MutableStateFlow(false)
    private val isAddSheetVisible = MutableStateFlow(false)
    private val editingEntry = MutableStateFlow<DrinkEntry?>(null)
    private val message = MutableStateFlow<UiMessage?>(null)
    private val messageId = AtomicLong()
    private val actionMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    // stateIn retains the loaded screen on resume; initial loading comes from HydrationUiState.
    private val overview = retryVersion.flatMapLatest {
        observeHydrationOverviewUseCase()
            .filter { overview -> isCurrentAccount() && (boundOwner == null ||
                overview.days.all { day -> day.entries.all { entry -> entry.ownerId == boundOwner } }) }
            .map<_, OverviewLoadState> { OverviewLoadState.Loaded(it) }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(OverviewLoadState.Failed)
            }
    }

    val state = combine(
        overview,
        isSaving,
        isAddSheetVisible,
        message,
        editingEntry
    ) { loaded, saving, sheetVisible, currentMessage, editing ->
        HydrationUiState(
            loading = loaded is OverviewLoadState.Loading,
            overview = (loaded as? OverviewLoadState.Loaded)?.overview,
            isSaving = saving,
            isAddSheetVisible = sheetVisible,
            message = currentMessage,
            editingEntry = editing,
            loadError = if (loaded is OverviewLoadState.Failed) "อ่านข้อมูลไม่สำเร็จ กรุณาลองใหม่" else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrationUiState())

    fun retry() {
        retryVersion.update { it + 1 }
    }

    fun showAddDrinkSheet() {
        if (!isSaving.value) {
            editingEntry.value = null
            isAddSheetVisible.value = true
        }
    }

    fun showEditDrinkSheet(entry: DrinkEntry) {
        if (!isSaving.value) {
            editingEntry.value = entry
            isAddSheetVisible.value = true
        }
    }

    fun saveDrink(drink: DrinkType, amountMl: Int) {
        val editing = editingEntry.value
        if (editing == null) add(drink, amountMl)
        else launchAction {
            editDrinkUseCase(editing.id, drink, amountMl)
            isAddSheetVisible.value = false
            editingEntry.value = null
            showMessage("แก้ไขรายการแล้ว")
        }
    }

    fun dismissAddDrinkSheet() {
        if (!isSaving.value) isAddSheetVisible.value = false
    }

    fun add(drink: DrinkType, amountMl: Int) = launchAction {
        addDrinkUseCase(drink, amountMl, boundOwner)
        isAddSheetVisible.value = false
        showMessage("บันทึกเครื่องดื่มแล้ว")
    }

    fun delete(id: Long) = launchAction {
        val deleted = deleteDrinkUseCase(id)
        if (deleted != null) message.value = UiMessage(messageId.incrementAndGet(), "ลบรายการแล้ว", deleted)
    }

    fun undoDelete(id: Long) {
        if (!isCurrentAccount()) return
        val pending = message.value?.takeIf { it.id == id && it.undoEntry != null } ?: return
        if (!message.compareAndSet(pending, null)) return
        viewModelScope.launch {
            // Queue a pressed Undo rather than dropping it while another write holds the mutex.
            actionMutex.withLock {
                if (!isCurrentAccount()) return@withLock
                isSaving.value = true
                try {
                    val restored = restoreDrinkUseCase(requireNotNull(pending.undoEntry))
                    showMessage(if (restored) "คืนรายการแล้ว" else "รายการนี้มีอยู่แล้ว")
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    message.value = pending.copy(id = messageId.incrementAndGet(), text = "คืนรายการไม่สำเร็จ ลองอีกครั้ง")
                } finally {
                    isSaving.value = false
                }
            }
        }
    }

    fun save(settings: HydrationSettings) = launchAction {
        val result = saveHydrationSettingsUseCase(settings)
        showMessage(
            if (result == SaveHydrationSettingsResult.SAVED) "บันทึกการตั้งค่าแล้ว"
            else "บันทึกการตั้งค่าแล้ว แต่ตั้งเวลาเตือนไม่สำเร็จ กรุณาลองบันทึกอีกครั้ง"
        )
    }

    fun clearMessage(id: Long) {
        message.update { if (it?.id == id) null else it }
    }

    private fun showMessage(text: String) {
        message.value = UiMessage(messageId.incrementAndGet(), text)
    }

    private fun launchAction(action: suspend () -> Unit) {
        if (!isCurrentAccount() || !actionMutex.tryLock()) return
        isSaving.value = true
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
                isSaving.value = false
                actionMutex.unlock()
            }
        }
    }
}
