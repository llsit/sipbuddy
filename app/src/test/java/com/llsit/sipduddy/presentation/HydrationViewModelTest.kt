package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModelStore
import com.llsit.sipduddy.domain.model.DateContext
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.usecase.AddDrinkUseCase
import com.llsit.sipduddy.domain.usecase.CalculateWeeklyHydrationUseCase
import com.llsit.sipduddy.domain.usecase.DeleteDrinkUseCase
import com.llsit.sipduddy.domain.usecase.EditDrinkUseCase
import com.llsit.sipduddy.domain.usecase.ObserveHydrationOverviewUseCase
import com.llsit.sipduddy.domain.usecase.RestoreDrinkUseCase
import com.llsit.sipduddy.domain.usecase.SaveHydrationSettingsUseCase
import com.llsit.sipduddy.fake.FakeDateProvider
import com.llsit.sipduddy.fake.FakeDrinkRepository
import com.llsit.sipduddy.fake.FakeHydrationSettingsRepository
import com.llsit.sipduddy.fake.FakeReminderScheduler
import com.llsit.sipduddy.presentation.intent.HydrationIntent
import com.llsit.sipduddy.rule.MainDispatcherRule
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HydrationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    private val date = LocalDate.of(2026, 10, 17)
    private val zone = ZoneId.of("Asia/Bangkok")
    private val clock = Clock.fixed(date.atTime(10, 0).atZone(zone).toInstant(), ZoneOffset.UTC)

    @After
    fun clearViewModels() {
        store.clear()
    }

    private fun TestScope.createViewModel(
        drinks: FakeDrinkRepository = FakeDrinkRepository(),
        settings: FakeHydrationSettingsRepository = FakeHydrationSettingsRepository(),
        scheduler: FakeReminderScheduler = FakeReminderScheduler(),
        collectState: Boolean = true,
        accounts: com.llsit.sipduddy.domain.repository.AccountRepository? = null,
    ): HydrationViewModel {
        if (accounts != null) settings.settings.value = settings.settings.value.copy(ownerId = accounts.owner.value)
        val observe = ObserveHydrationOverviewUseCase(
            drinks,
            settings,
            FakeDateProvider(DateContext(date, zone)),
            CalculateWeeklyHydrationUseCase(),
            StandardTestDispatcher(testScheduler),
        )
        val viewModel = HydrationViewModel(
            observe,
            AddDrinkUseCase(drinks, clock),
            DeleteDrinkUseCase(drinks),
            SaveHydrationSettingsUseCase(settings, scheduler),
            EditDrinkUseCase(drinks),
            RestoreDrinkUseCase(drinks),
            accounts,
        )
        store.put("subject", viewModel)
        if (collectState) backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect { } }
        runCurrent()
        return viewModel
    }

    @Test
    fun loadsDataAndClosesSheetOnlyAfterSuccessfulAdd() = runTest {
        val repository = FakeDrinkRepository()
        val viewModel = createViewModel(repository)
        assertFalse(viewModel.state.value.loading)
        viewModel.onIntent(HydrationIntent.OpenAddDrink)
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 250))
        runCurrent()
        assertEquals(250L, viewModel.state.value.overview?.totalMl)
        assertFalse(viewModel.state.value.isAddSheetVisible)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun writeFailureKeepsSheetOpenAndAllowsAnotherAttempt() = runTest {
        val repository = FakeDrinkRepository().apply { writeFailure = IOException("disk full") }
        val viewModel = createViewModel(repository)
        viewModel.onIntent(HydrationIntent.OpenAddDrink)
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 250))
        runCurrent()
        assertTrue(viewModel.state.value.isAddSheetVisible)
        assertNotNull(viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSaving)
        repository.writeFailure = null
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 250))
        runCurrent()
        assertFalse(viewModel.state.value.isAddSheetVisible)
    }

    @Test
    fun duplicateSubmissionsAreRejectedBeforeCoroutineStarts() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeDrinkRepository().apply { addGate = gate }
        val viewModel = createViewModel(repository)
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 250))
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 250))
        runCurrent()
        assertEquals(1, repository.addCalls)
        assertTrue(viewModel.state.value.isSaving)
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, repository.entries.value.size)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun loadFailureCanRetryWithoutRestartingApp() = runTest {
        val repository =
            FakeDrinkRepository().apply { observationFailure = IOException("read failed") }
        val viewModel = createViewModel(repository)
        assertNotNull(viewModel.state.value.loadError)
        repository.observationFailure = null
        viewModel.onIntent(HydrationIntent.Retry)
        runCurrent()
        assertNull(viewModel.state.value.loadError)
        assertNotNull(viewModel.state.value.overview)
    }

    @Test
    fun oldSnackbarAcknowledgementCannotClearNewMessage() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 0))
        runCurrent()
        val firstId = viewModel.state.value.message!!.id
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 0))
        runCurrent()
        val secondId = viewModel.state.value.message!!.id
        assertNotEquals(firstId, secondId)
        viewModel.onIntent(HydrationIntent.MessageHandled(firstId))
        runCurrent()
        assertEquals(secondId, viewModel.state.value.message?.id)
        viewModel.onIntent(HydrationIntent.MessageHandled(secondId))
        runCurrent()
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun schedulingFailureReportsPartialSuccess() = runTest {
        val settings = FakeHydrationSettingsRepository()
        val scheduler = FakeReminderScheduler().apply { failure = SecurityException() }
        val viewModel = createViewModel(settings = settings, scheduler = scheduler)
        viewModel.onIntent(HydrationIntent.SaveSettings(HydrationSettings(3000)))
        runCurrent()
        assertEquals(3000, viewModel.state.value.overview?.settings?.goalMl)
        assertTrue(viewModel.state.value.message!!.text.contains("ตั้งเวลาเตือนไม่สำเร็จ"))
    }

    @Test
    fun cancellationDoesNotBecomeAnErrorAndReleasesBusyState() = runTest {
        val repository = FakeDrinkRepository().apply { writeFailure = CancellationException() }
        val viewModel = createViewModel(repository)
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 250))
        runCurrent()
        assertNull(viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSaving)
        repository.writeFailure = null
        viewModel.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 250))
        runCurrent()
        assertEquals(1, repository.entries.value.size)
    }

    @Test fun undoRestoresRecordAndIgnoresDuplicateOrStaleActions() = runTest {
        val entry = DrinkEntry(8, DrinkType.TEA, 300, clock.millis())
        val repo = FakeDrinkRepository().apply { entries.value = listOf(entry) }
        val vm = createViewModel(repo)
        vm.onIntent(HydrationIntent.DeleteDrink(8))
        runCurrent()
        val id = vm.state.value.message!!.id
        assertTrue(repo.entries.value.isEmpty())
        vm.onIntent(HydrationIntent.UndoDelete(id - 1))
        runCurrent()
        assertTrue(repo.entries.value.isEmpty())
        vm.onIntent(HydrationIntent.UndoDelete(id))
        vm.onIntent(HydrationIntent.UndoDelete(id))
        runCurrent()
        assertEquals(listOf(entry), repo.entries.value)
        assertEquals(300L, vm.state.value.overview!!.totalMl)
    }

    @Test fun undoFailureOffersRetryAndRetainsSnapshot() = runTest {
        val entry = DrinkEntry(8, DrinkType.WATER, 250, clock.millis())
        val repo = FakeDrinkRepository().apply { entries.value = listOf(entry) }
        val vm = createViewModel(repo)
        vm.onIntent(HydrationIntent.DeleteDrink(8))
        runCurrent()
        repo.writeFailure = IOException()
        vm.onIntent(HydrationIntent.UndoDelete(vm.state.value.message!!.id))
        runCurrent()
        assertEquals(entry, vm.state.value.message!!.undoEntry)
        repo.writeFailure = null
        vm.onIntent(HydrationIntent.UndoDelete(vm.state.value.message!!.id))
        runCurrent()
        assertEquals(listOf(entry), repo.entries.value)
    }

    @Test fun editUsesExistingRecordAndFailureKeepsEditorOpen() = runTest {
        val entry = DrinkEntry(8, DrinkType.WATER, 250, clock.millis() - 86400000)
        val repo = FakeDrinkRepository().apply { entries.value = listOf(entry) }
        val vm = createViewModel(repo)
        vm.onIntent(HydrationIntent.OpenEditDrink(entry))
        repo.writeFailure = IOException()
        vm.onIntent(HydrationIntent.SaveDrink(DrinkType.MILK, 400))
        runCurrent()
        assertTrue(vm.state.value.isAddSheetVisible)
        repo.writeFailure = null
        vm.onIntent(HydrationIntent.SaveDrink(DrinkType.MILK, 400))
        runCurrent()
        assertFalse(vm.state.value.isAddSheetVisible)
        assertEquals(listOf(entry.copy(drink = DrinkType.MILK, amountMl = 400)), repo.entries.value)
        assertEquals(0L, vm.state.value.overview!!.totalMl)
    }

    @Test fun pressedUndoWaitsForInFlightWrite() = runTest {
        val entry = DrinkEntry(8, DrinkType.WATER, 250, clock.millis())
        val repo = FakeDrinkRepository().apply { entries.value = listOf(entry) }
        val vm = createViewModel(repo)
        vm.onIntent(HydrationIntent.DeleteDrink(8))
        runCurrent()
        val id = vm.state.value.message!!.id
        val gate = CompletableDeferred<Unit>()
        repo.addGate = gate
        vm.onIntent(HydrationIntent.SaveDrink(DrinkType.TEA, 150))
        runCurrent()
        vm.onIntent(HydrationIntent.UndoDelete(id))
        runCurrent()
        assertTrue(repo.entries.value.isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertTrue(entry in repo.entries.value)
        assertEquals(2, repo.entries.value.size)
    }

    @Test fun returningFromBackgroundKeepsLoadedScreenWhileQueryRestarts() = runTest {
        val repo = FakeDrinkRepository()
        val vm = createViewModel(repo, collectState = false)
        val subscription = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        runCurrent()
        assertNotNull(vm.state.value.overview)
        subscription.cancel()
        testScheduler.advanceTimeBy(5_001)
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        repo.observationGate = gate
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        runCurrent()
        assertFalse("Reload must not remove the settings screen and its unsaved draft", vm.state.value.loading)
        assertNotNull(vm.state.value.overview)
        gate.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.loading)
    }

    @Test fun oldAccountViewModelRejectsForeignEmissionsAndQueuedWrites() = runTest {
        val accounts = com.llsit.sipduddy.fake.FakeAccountRepository("A")
        val own = DrinkEntry(1, DrinkType.WATER, 250, clock.millis(), ownerId = "A")
        val repo = FakeDrinkRepository().apply { entries.value = listOf(own) }
        val vm = createViewModel(repo, accounts = accounts)
        assertEquals(250L, vm.state.value.overview?.totalMl)
        accounts.owner.value = "B"
        val foreign = own.copy(id = 2, amountMl = 999, ownerId = "B")
        repo.entries.value = listOf(foreign)
        vm.onIntent(HydrationIntent.SaveDrink(DrinkType.TEA, 150))
        runCurrent()
        assertEquals(listOf(foreign), repo.entries.value)
        assertEquals(250L, vm.state.value.overview?.totalMl)
        // Even a late B emission after switching back cannot enter A's retained state.
        accounts.owner.value = "A"
        repo.entries.value = listOf(foreign.copy(amountMl = 1000))
        runCurrent()
        assertEquals(250L, vm.state.value.overview?.totalMl)
    }

    @Test fun pendingEffectsSurviveAbsentCollectorAndStopReplayingAfterAcknowledgement() = runTest {
        val vm = createViewModel(collectState = false)
        vm.onIntent(HydrationIntent.SaveDrink(DrinkType.WATER, 0))
        runCurrent()
        val first = vm.effects.first() as com.llsit.sipduddy.presentation.effect.HydrationEffect.ShowMessage
        val replay = vm.effects.first()
        assertEquals(first, replay)
        vm.onIntent(HydrationIntent.MessageHandled(first.message.id))
        assertEquals(com.llsit.sipduddy.presentation.effect.HydrationEffect.DismissMessage, vm.effects.first())
    }

    @Test fun navigationDeleteConfirmationAndEditorAreDrivenByIntents() = runTest {
        val vm = createViewModel()
        vm.onIntent(HydrationIntent.Navigate(com.llsit.sipduddy.presentation.model.AppDestination.HISTORY))
        vm.onIntent(HydrationIntent.RequestDelete(8))
        assertEquals(com.llsit.sipduddy.presentation.model.AppDestination.HISTORY, vm.state.value.destination)
        assertEquals(8L, vm.state.value.deletingId)
        vm.onIntent(HydrationIntent.DismissDelete)
        assertNull(vm.state.value.deletingId)
        vm.onIntent(HydrationIntent.OpenAddDrink)
        assertTrue(vm.state.value.isAddSheetVisible)
        vm.onIntent(HydrationIntent.DismissDrinkEditor)
        assertFalse(vm.state.value.isAddSheetVisible)
    }
}
