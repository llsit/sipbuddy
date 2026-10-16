package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DateContext
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.model.HydrationOverview
import com.llsit.sipduddy.fake.FakeDateProvider
import com.llsit.sipduddy.fake.FakeDrinkRepository
import com.llsit.sipduddy.fake.FakeHydrationSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveHydrationOverviewUseCaseTest {
    private val date = LocalDate.of(2026, 10, 17)
    private val zone = ZoneId.of("Asia/Bangkok")
    @Test
    fun queriesOnlyVisibleWindowAndObservesSettings() = runTest {
        val repository = FakeDrinkRepository()
        val settings = FakeHydrationSettingsRepository()
        val useCase = ObserveHydrationOverviewUseCase(
            repository,
            settings,
            FakeDateProvider(DateContext(date, zone)),
            CalculateWeeklyHydrationUseCase(),
            StandardTestDispatcher(testScheduler)
        )
        assertEquals(7, useCase().first().days.size)
        assertEquals(
            date.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli(),
            repository.lastStart
        )
        assertEquals(
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            repository.lastEnd
        )
    }

    @Test
    fun reQueriesAndRegroupsAfterTimezoneAndDateChanges() = runTest {
        val repository = FakeDrinkRepository()
        repository.entries.value = listOf(
            DrinkEntry(
                1,
                DrinkType.WATER,
                250,
                Instant.parse("2026-10-16T18:00:00Z").toEpochMilli()
            )
        )
        val dates = FakeDateProvider(DateContext(date, zone))
        val values = mutableListOf<HydrationOverview>()
        val useCase = ObserveHydrationOverviewUseCase(
            repository,
            FakeHydrationSettingsRepository(),
            dates,
            CalculateWeeklyHydrationUseCase(),
            StandardTestDispatcher(testScheduler)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect { values += it } }
        runCurrent()
        assertEquals(250L, values.last().totalMl)
        dates.dates.value = DateContext(date, ZoneOffset.UTC)
        runCurrent()
        assertEquals(0L, values.last().totalMl)
        assertEquals(250L, values.last().days[5].totalMl)
        dates.dates.value = DateContext(date.plusDays(1), zone)
        runCurrent()
        assertEquals(date.plusDays(1), values.last().dateContext.date)
    }
}
