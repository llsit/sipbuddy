package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.ReminderTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class CalculateNextReminderUseCaseTest {
    private val useCase = CalculateNextReminderUseCase()
    @Test
    fun choosesTodayBeforeTimeAndTomorrowAtOrAfterTime() {
        val now = ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, ZoneId.of("Asia/Bangkok"))
        assertEquals(now, useCase(now.minusMinutes(1), ReminderTime()))
        assertEquals(now.plusDays(1), useCase(now, ReminderTime()))
        assertEquals(now.plusDays(1), useCase(now.plusHours(1), ReminderTime()))
    }

    @Test
    fun retainsWallClockAcrossDaylightSaving() {
        val now = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, ZoneId.of("America/New_York"))
        val next = useCase(now, ReminderTime())
        assertEquals(10, next.hour)
        assertEquals(21L, Duration.between(now, next).toHours())
    }

    @Test
    fun resolvesNonexistentSpringForwardTimeToNextValidTime() {
        val now = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, ZoneId.of("America/New_York"))
        assertEquals(3, useCase(now, ReminderTime(2, 30)).hour)
    }
}
