package com.llsit.sipduddy.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llsit.sipduddy.domain.usecase.ImportGuestHistoryUseCase
import com.llsit.sipduddy.domain.usecase.ObserveSyncStatusUseCase
import com.llsit.sipduddy.domain.usecase.RequestHydrationSyncUseCase
import com.llsit.sipduddy.presentation.model.SyncUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class SyncViewModel(observe: ObserveSyncStatusUseCase,private val request: RequestHydrationSyncUseCase,
    private val importGuestUseCase: ImportGuestHistoryUseCase,private val expectedOwner: String? = null) : ViewModel() {
    private val importing=MutableStateFlow(false)
    private val message=MutableStateFlow<String?>(null)
    val state=combine(observe().filter { expectedOwner == null || it.ownerId == expectedOwner },importing,message) { status,busy,text -> SyncUiState(status,busy,text) }
        .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),SyncUiState())
    fun sync() = request()
    fun importGuest(owner: String) {
        if (!importing.compareAndSet(false,true)) return
        viewModelScope.launch {
            try { message.value="ย้ายประวัติ ${importGuestUseCase(owner)} รายการเข้าบัญชีแล้ว" }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message.value="ย้ายประวัติไม่สำเร็จ กรุณาลองใหม่" }
            finally { importing.value=false }
        }
    }
}
