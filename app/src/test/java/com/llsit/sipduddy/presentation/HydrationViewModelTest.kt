package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModelStore
import com.llsit.sipduddy.domain.model.DateContext
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.usecase.AddDrinkUseCase
import com.llsit.sipduddy.domain.usecase.CalculateWeeklyHydrationUseCase
import com.llsit.sipduddy.domain.usecase.DeleteDrinkUseCase
import com.llsit.sipduddy.domain.usecase.ObserveHydrationOverviewUseCase
import com.llsit.sipduddy.domain.usecase.SaveHydrationSettingsUseCase
import com.llsit.sipduddy.fake.FakeDateProvider
import com.llsit.sipduddy.fake.FakeDrinkRepository
import com.llsit.sipduddy.fake.FakeHydrationSettingsRepository
import com.llsit.sipduddy.fake.FakeReminderScheduler
import com.llsit.sipduddy.rule.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

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
    ): HydrationViewModel {
        val observe = ObserveHydrationOverviewUseCase(
            drinks,
            settings,
            FakeDateProvider(DateContext(date, zone)),
            CalculateWeeklyHydrationUseCase(),
            StandardTestDispatcher(testScheduler)
        )
        val viewModel = HydrationViewModel(
            observe,
            AddDrinkUseCase(drinks, clock),
            DeleteDrinkUseCase(drinks),
            SaveHydrationSettingsUseCase(settings, scheduler)
        )
        store.put("subject", viewModel)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect { } }
        runCurrent()
        return viewModel
    }

    @Test
    fun loadsDataAndClosesSheetOnlyAfterSuccessfulAdd() = runTest {
        val repository = FakeDrinkRepository()
        val viewModel = createViewModel(repository)
        assertFalse(viewModel.state.value.loading)
        viewModel.showAddDrinkSheet()
        viewModel.add(DrinkType.WATER, 250)
        runCurrent()
        assertEquals(250L, viewModel.state.value.overview?.totalMl)
        assertFalse(viewModel.state.value.isAddSheetVisible)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun writeFailureKeepsSheetOpenAndAllowsAnotherAttempt() = runTest {
        val repository = FakeDrinkRepository().apply { writeFailure = IOException("disk full") }
        val viewModel = createViewModel(repository)
        viewModel.showAddDrinkSheet()
        viewModel.add(DrinkType.WATER, 250)
        runCurrent()
        assertTrue(viewModel.state.value.isAddSheetVisible)
        assertNotNull(viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSaving)
        repository.writeFailure = null
        viewModel.add(DrinkType.WATER, 250)
        runCurrent()
        assertFalse(viewModel.state.value.isAddSheetVisible)
    }

    @Test
    fun duplicateSubmissionsAreRejectedBeforeCoroutineStarts() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeDrinkRepository().apply { addGate = gate }
        val viewModel = createViewModel(repository)
        viewModel.add(DrinkType.WATER, 250)
        viewModel.add(DrinkType.WATER, 250)
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
        viewModel.retry()
        runCurrent()
        assertNull(viewModel.state.value.loadError)
        assertNotNull(viewModel.state.value.overview)
    }

    @Test
    fun oldSnackbarAcknowledgementCannotClearNewMessage() = runTest {
        val viewModel = createViewModel()
        viewModel.add(DrinkType.WATER, 0)
        runCurrent()
        val firstId = viewModel.state.value.message!!.id
        viewModel.add(DrinkType.WATER, 0)
        runCurrent()
        val secondId = viewModel.state.value.message!!.id
        assertNotEquals(firstId, secondId)
        viewModel.clearMessage(firstId)
        runCurrent()
        assertEquals(secondId, viewModel.state.value.message?.id)
        viewModel.clearMessage(secondId)
        runCurrent()
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun schedulingFailureReportsPartialSuccess() = runTest {
        val settings = FakeHydrationSettingsRepository()
        val scheduler = FakeReminderScheduler().apply { failure = SecurityException() }
        val viewModel = createViewModel(settings = settings, scheduler = scheduler)
        viewModel.save(HydrationSettings(3000))
        runCurrent()
        assertEquals(3000, viewModel.state.value.overview?.settings?.goalMl)
        assertTrue(viewModel.state.value.message!!.text.contains("ตั้งเวลาเตือนไม่สำเร็จ"))
    }

    @Test
    fun cancellationDoesNotBecomeAnErrorAndReleasesBusyState() = runTest {
        val repository = FakeDrinkRepository().apply { writeFailure = CancellationException() }
        val viewModel = createViewModel(repository)
        viewModel.add(DrinkType.WATER, 250)
        runCurrent()
        assertNull(viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSaving)
        repository.writeFailure = null
        viewModel.add(DrinkType.WATER, 250)
        runCurrent()
        assertEquals(1, repository.entries.value.size)
    }
}
