package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.fake.FakeDrinkRepository
import com.llsit.sipduddy.fake.FakeHydrationSettingsRepository
import java.time.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class ReminderPolicyUseCaseTest {
    private val zone = ZoneId.of("Asia/Bangkok")
    private val day = LocalDate.of(2026, 10, 17)
    private fun at(hour: Int, minute: Int = 0) = day.atTime(hour, minute).atZone(zone)

    @Test fun unsortedMultipleSlotsSelectNextAndRollover() {
        val settings = ReminderSettings(true, listOf(ReminderTime(18, 0), ReminderTime(10, 0), ReminderTime(14, 0)))
        val next = CalculateNextScheduledReminderUseCase()
        assertEquals(at(14), next(at(10), settings))
        assertEquals(at(10).plusDays(1), next(at(18), settings))
    }

    @Test fun quietHoursExcludeStartAndAllowEndAcrossMidnight() {
        val quiet = QuietHours()
        assertTrue(quiet.contains(LocalTime.of(22, 0)))
        assertTrue(quiet.contains(LocalTime.of(6, 59)))
        assertFalse(quiet.contains(LocalTime.of(7, 0)))
        assertFalse(quiet.contains(LocalTime.of(21, 59)))
        val settings = ReminderSettings(true, listOf(ReminderTime(23, 0), ReminderTime(7, 0)))
        assertEquals(at(7).plusDays(1), CalculateNextScheduledReminderUseCase()(at(21), settings))
    }

    @Test fun daytimeQuietAndEqualBoundsHaveDefinedBehavior() {
        val quiet = QuietHours(true, ReminderTime(12, 0), ReminderTime(14, 0))
        assertTrue(quiet.contains(LocalTime.of(13, 0)))
        assertFalse(quiet.contains(LocalTime.of(14, 0)))
        assertFalse(quiet.copy(end = quiet.start).contains(LocalTime.of(12, 0)))
    }

    @Test fun disabledReturnsNoAlarmAndInvalidSchedulesAreRejected() {
        assertNull(CalculateNextScheduledReminderUseCase()(at(10), ReminderSettings()))
        for (times in listOf(emptyList(), listOf(ReminderTime(10, 0), ReminderTime(10, 0)), listOf(ReminderTime(23, 0)))) {
            try {
                ReminderSettings(true, times)
                fail("accepted invalid schedule")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun dstSpringGapStillSchedulesFutureWallClockCandidate() {
        val now = ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, ZoneId.of("America/New_York"))
        val settings = ReminderSettings(true, listOf(ReminderTime(2, 30)), QuietHours(enabled = false))
        val next = CalculateNextScheduledReminderUseCase()(now, settings)!!
        assertEquals(3, next.hour)
        assertEquals(30, next.minute)
        assertTrue(next.isAfter(now))
    }
    private suspend fun allowed(repo: FakeDrinkRepository, settings: HydrationSettings, now: ZonedDateTime = at(10)): Boolean {
        val prefs = FakeHydrationSettingsRepository().apply { this.settings.value = settings }
        return ShouldSendReminderUseCase(repo, prefs, Clock.fixed(now.toInstant(), ZoneOffset.UTC), { zone })()
    }

    @Test fun goalSuppressionUsesOnlyCurrentLocalDayAndResumesTomorrow() = runTest {
        val repo = FakeDrinkRepository().apply {
            entries.value = listOf(
                DrinkEntry(1, DrinkType.WATER, 2000, at(8).toInstant().toEpochMilli()),
                DrinkEntry(2, DrinkType.TEA, 5000, at(10).minusDays(1).toInstant().toEpochMilli()),
                DrinkEntry(3, DrinkType.WATER, 5000, day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()),
            )
        }
        val prefs = HydrationSettings(reminder = ReminderSettings(enabled = true))
        assertFalse(allowed(repo, prefs))
        repo.entries.value = repo.entries.value.take(1)
        assertTrue(allowed(repo, prefs, at(10).plusDays(1)))
        EditDrinkUseCase(repo)(1, DrinkType.WATER, 1999)
        assertTrue(allowed(repo, prefs))
    }

    @Test fun quietAndDisabledSuppressEvenWhenGoalSwitchIsOff() = runTest {
        val repo = FakeDrinkRepository()
        val prefs = HydrationSettings(reminder = ReminderSettings(enabled = true, stopAtGoal = false))
        assertFalse(allowed(repo, prefs, at(23)))
        assertFalse(allowed(repo, prefs.copy(reminder = prefs.reminder.copy(enabled = false))))
        repo.entries.value = listOf(DrinkEntry(1, DrinkType.WATER, 5000, at(8).toInstant().toEpochMilli()))
        assertTrue(allowed(repo, prefs))
    }

    @Test fun eligibilityUsesAggregateQueryAndAvoidsReadingDrinksWhenUnnecessary() = runTest {
        val repo = FakeDrinkRepository()
        val prefs = HydrationSettings(reminder = ReminderSettings(enabled = true))
        assertTrue(allowed(repo, prefs))
        assertEquals(1, repo.totalCalls)
        assertEquals(0, repo.observationCalls)
        assertFalse(allowed(repo, prefs, at(23)))
        assertTrue(allowed(repo, prefs.copy(reminder = prefs.reminder.copy(stopAtGoal = false))))
        assertEquals(1, repo.totalCalls)
    }
}
