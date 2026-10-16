package com.llsit.sipduddy

import com.llsit.sipduddy.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class HydrationTest {
    private val zone = ZoneId.of("Asia/Bangkok")
    private val today = LocalDate.of(2026, 10, 17)
    private fun entry(date: LocalDate, ml: Int) = DrinkEntry(drink = Drink.WATER, amountMl = ml, timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli())
    @Test fun weekIncludesSevenDaysAndLocalMidnight() {
        val entries = listOf(entry(today, 250), entry(today, 150), entry(today.minusDays(1), 500), entry(today.minusDays(7), 999))
        val totals = weeklyTotals(entries, today, zone)
        assertEquals(7, totals.size)
        assertEquals(today.minusDays(6), totals.first().date)
        assertEquals(400, totals.last().totalMl)
        assertEquals(500, totals[5].totalMl)
        assertEquals(900, totals.sumOf { it.totalMl })
    }
    @Test fun emptyWeekAndProgressBoundaries() {
        assertTrue(weeklyTotals(emptyList(), today, zone).all { it.totalMl == 0 })
        assertEquals(1f, progress(2500, 2000))
        assertEquals(.5f, progress(1000, 2000))
        assertEquals(0f, progress(100, 0))
    }
    @Test fun reminderBeforeAtAndAfterChosenTime() {
        val now = today.atTime(10, 0).atZone(zone)
        assertEquals(today, nextReminder(now.minusMinutes(1), 10, 0).toLocalDate())
        assertEquals(today.plusDays(1), nextReminder(now, 10, 0).toLocalDate())
        assertEquals(today.plusDays(1), nextReminder(now.plusHours(1), 10, 0).toLocalDate())
    }
    @Test fun reminderRetainsWallTimeAcrossDaylightSaving() {
        val now = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, ZoneId.of("America/New_York"))
        val next = nextReminder(now, 10, 0)
        assertEquals(10, next.hour)
        assertEquals(21, Duration.between(now, next).toHours().toInt())
    }
    @Test fun addValidatesAndDeleteUpdatesRepository() = runBlocking {
        val repository = FakeRepository()
        val add = AddDrink(repository)
        for (invalid in listOf(-1, 0, 5001)) {
            try { add(Drink.WATER, invalid); fail("invalid amount accepted") } catch (_: IllegalArgumentException) { }
        }
        add(Drink.TEA, 250, 123L)
        assertEquals(Drink.TEA, repository.entries.value.single().drink)
        assertEquals(123L, repository.entries.value.single().timestamp)
        DeleteDrink(repository)(1)
        assertTrue(repository.entries.value.isEmpty())
    }
    @Test fun settingsValidateBeforePersistenceAndScheduling() = runBlocking {
        val repository = FakeRepository()
        var scheduled: Preferences? = null
        val save = SavePreferences(repository, object : ReminderScheduler { override fun schedule(preferences: Preferences) { scheduled = preferences } })
        for (prefs in listOf(Preferences(goalMl = 0), Preferences(hour = 24), Preferences(minute = 60))) {
            try { save(prefs); fail("invalid preferences accepted") } catch (_: IllegalArgumentException) { }
        }
        assertNull(scheduled)
        val valid = Preferences(2500, true, 9, 30)
        save(valid)
        assertEquals(valid, repository.preferences.value)
        assertEquals(valid, scheduled)
    }
    private class FakeRepository : HydrationRepository {
        override val entries = MutableStateFlow<List<DrinkEntry>>(emptyList())
        override val preferences = MutableStateFlow(Preferences())
        override suspend fun add(entry: DrinkEntry) { entries.value = entries.value + entry.copy(id = 1) }
        override suspend fun delete(id: Long) { entries.value = entries.value.filterNot { it.id == id } }
        override suspend fun savePreferences(preferences: Preferences) { this.preferences.value = preferences }
    }
}
