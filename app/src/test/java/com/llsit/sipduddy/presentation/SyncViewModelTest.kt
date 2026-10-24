package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModelStore
import com.llsit.sipduddy.domain.model.SyncStatus
import com.llsit.sipduddy.domain.repository.HydrationSyncRepository
import com.llsit.sipduddy.domain.usecase.*
import com.llsit.sipduddy.fake.FakeAccountRepository
import com.llsit.sipduddy.presentation.intent.SyncIntent
import com.llsit.sipduddy.rule.MainDispatcherRule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val accounts = FakeAccountRepository("A")
    private val store = ViewModelStore()
    private val statuses = MutableStateFlow(SyncStatus(ownerId = "A"))
    private var requests = 0
    private var imports = 0
    private var gate: CompletableDeferred<Unit>? = null
    private var failure: Exception? = null
    private val repository = object : HydrationSyncRepository {
        override val status = statuses
        override fun requestSync() {
            requests++
        }
        override suspend fun importGuestHistory(expectedOwner: String): Int {
            imports++
            gate?.await()
            failure?.let { throw it }
            return 4
        }
    }
    private fun vm() = SyncViewModel(
        ObserveSyncStatusUseCase(repository),
        RequestHydrationSyncUseCase(repository),
        ImportGuestHistoryUseCase(repository),
        "A",
        accounts,
    ).also { store.put("sync", it) }

    @After fun clear() {
        store.clear()
    }

    @Test fun importIsReservedBeforeCoroutineStartsAndDuplicateIsIgnored() = runTest {
        val vm = vm()
        runCurrent()
        gate = CompletableDeferred()
        vm.onIntent(SyncIntent.ImportGuestHistory("A"))
        vm.onIntent(SyncIntent.ImportGuestHistory("A"))
        assertTrue(vm.state.value.importing)
        runCurrent()
        assertEquals(1, imports)
        statuses.value = SyncStatus(ownerId = "A", pending = 7)
        runCurrent()
        assertEquals(7, vm.state.value.status.pending)
        assertTrue(vm.state.value.importing)
        gate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.importing)
        assertTrue(vm.state.value.message!!.contains("4"))
    }

    @Test fun staleStoreRejectsForeignEventsCommandsAndImportOwners() = runTest {
        val vm = vm()
        runCurrent()
        vm.onIntent(SyncIntent.ImportGuestHistory("B"))
        runCurrent()
        assertEquals(0, imports)
        accounts.owner.value = "B"
        statuses.value = SyncStatus(ownerId = "B", pending = 99)
        vm.onIntent(SyncIntent.SyncNow)
        vm.onIntent(SyncIntent.ImportGuestHistory("B"))
        runCurrent()
        assertEquals(0, requests)
        assertEquals(0, imports)
        assertEquals("A", vm.state.value.status.ownerId)
    }

    @Test fun cancelledImportReleasesBusyWithoutErrorMessage() = runTest {
        val vm = vm()
        runCurrent()
        failure = CancellationException()
        vm.onIntent(SyncIntent.ImportGuestHistory("A"))
        runCurrent()
        assertFalse(vm.state.value.importing)
        assertNull(vm.state.value.message)
        failure = null
        vm.onIntent(SyncIntent.ImportGuestHistory("A"))
        runCurrent()
        assertEquals(2, imports)
        assertNotNull(vm.state.value.message)
    }

    @Test fun manualSyncUsesIntentAndPreservesUiState() = runTest {
        val vm = vm()
        runCurrent()
        val before = vm.state.value
        vm.onIntent(SyncIntent.SyncNow)
        assertEquals(1, requests)
        assertEquals(before, vm.state.value)
    }
}
