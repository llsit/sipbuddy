package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
) : ViewModel() {
    private val retryVersion = MutableStateFlow(0)
    private val isSaving = MutableStateFlow(false)
    private val isAddSheetVisible = MutableStateFlow(false)
    private val message = MutableStateFlow<UiMessage?>(null)
    private val messageId = AtomicLong()
    private val actionMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val overview = retryVersion.flatMapLatest {
        observeHydrationOverviewUseCase()
            .map<_, OverviewLoadState> { OverviewLoadState.Loaded(it) }
            .onStart { emit(OverviewLoadState.Loading) }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(OverviewLoadState.Failed)
            }
    }

    val state = combine(
        overview,
        isSaving,
        isAddSheetVisible,
        message
    ) { loaded, saving, sheetVisible, currentMessage ->
        HydrationUiState(
            loading = loaded is OverviewLoadState.Loading,
            overview = (loaded as? OverviewLoadState.Loaded)?.overview,
            isSaving = saving,
            isAddSheetVisible = sheetVisible,
            message = currentMessage,
            loadError = if (loaded is OverviewLoadState.Failed) "อ่านข้อมูลไม่สำเร็จ กรุณาลองใหม่" else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrationUiState())

    fun retry() {
        retryVersion.update { it + 1 }
    }

    fun showAddDrinkSheet() {
        if (!isSaving.value) isAddSheetVisible.value = true
    }

    fun dismissAddDrinkSheet() {
        if (!isSaving.value) isAddSheetVisible.value = false
    }

    fun add(drink: DrinkType, amountMl: Int) = launchAction {
        addDrinkUseCase(drink, amountMl)
        isAddSheetVisible.value = false
        showMessage("บันทึกเครื่องดื่มแล้ว")
    }

    fun delete(id: Long) = launchAction {
        deleteDrinkUseCase(id)
        showMessage("ลบรายการแล้ว")
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
        if (!actionMutex.tryLock()) return
        isSaving.value = true
        viewModelScope.launch {
            try {
                action()
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
