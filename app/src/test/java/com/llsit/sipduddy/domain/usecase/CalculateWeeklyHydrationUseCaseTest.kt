package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DateContext
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.model.HydrationOverview
import com.llsit.sipduddy.domain.model.HydrationSettings
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateWeeklyHydrationUseCaseTest {
    private val context = DateContext(LocalDate.of(2026, 10, 17), ZoneId.of("Asia/Bangkok"))
    private val useCase = CalculateWeeklyHydrationUseCase()
    private fun entry(offset: Long, amount: Int = 250, id: Long = 1) = DrinkEntry(
        id,
        DrinkType.WATER,
        amount,
        context.date.plusDays(offset).atStartOfDay(context.zone).toInstant().toEpochMilli(),
    )

    @Test
    fun includesSevenLocalCalendarDaysAndExcludesFutureAndOldRecords() {
        val days = useCase(
            listOf(entry(0), entry(-1, 150), entry(-6, 500), entry(-7, 999), entry(1, 999)),
            context,
        )
        assertEquals(7, days.size)
        assertEquals(500L, days.first().totalMl)
        assertEquals(250L, days.last().totalMl)
        assertEquals(900L, days.sumOf { it.totalMl })
    }

    @Test
    fun exactMidnightBelongsToNewDay() {
        val midnight = entry(0)
        val days =
            useCase(listOf(midnight, midnight.copy(timestamp = midnight.timestamp - 1)), context)
        assertEquals(250L, days.last().totalMl)
        assertEquals(250L, days[5].totalMl)
    }

    @Test
    fun fillsEmptyDaysAndSortsEqualTimestampsByIdDescending() {
        val days = useCase(listOf(entry(0, id = 1), entry(0, id = 2)), context)
        assertTrue(days.take(6).all { it.entries.isEmpty() })
        assertEquals(listOf(2L, 1L), days.last().entries.map { it.id })
    }

    @Test
    fun longTotalsDoNotOverflowAndOverviewProgressIsClamped() {
        val days = useCase(listOf(entry(0, Int.MAX_VALUE), entry(0, Int.MAX_VALUE, 2)), context)
        val overview = HydrationOverview(context, HydrationSettings(), days)
        assertEquals(4294967294L, overview.totalMl)
        assertEquals(1f, overview.progress)
        assertEquals(0L, overview.remainingMl)
    }

    @Test
    fun partialProgressAndRemainingAreConsistent() {
        val overview = HydrationOverview(
            context,
            HydrationSettings(),
            useCase(listOf(entry(0, 1000)), context),
        )
        assertEquals(.5f, overview.progress)
        assertEquals(1000L, overview.remainingMl)
    }
}
