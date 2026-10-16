package com.llsit.sipduddy.domain

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Drink(val title: String, val symbol: String) {
    WATER("น้ำเปล่า", "◉"), TEA("ชา", "❋"), COFFEE("กาแฟ", "☕"), MILK("นม", "♡"), JUICE("น้ำผลไม้", "✿")
}
data class DrinkEntry(val id: Long = 0, val drink: Drink, val amountMl: Int, val timestamp: Long)
data class Preferences(val goalMl: Int = 2000, val reminderEnabled: Boolean = false, val hour: Int = 10, val minute: Int = 0)
data class DailyTotal(val date: LocalDate, val totalMl: Int)

interface HydrationRepository {
    val entries: Flow<List<DrinkEntry>>
    val preferences: Flow<Preferences>
    suspend fun add(entry: DrinkEntry)
    suspend fun delete(id: Long)
    suspend fun savePreferences(preferences: Preferences)
}
interface ReminderScheduler { fun schedule(preferences: Preferences) }

class AddDrink(private val repository: HydrationRepository) {
    suspend operator fun invoke(drink: Drink, amountMl: Int, timestamp: Long = System.currentTimeMillis()) {
        require(amountMl in 1..5000) { "ระบุปริมาณ 1–5,000 มล." }
        repository.add(DrinkEntry(drink = drink, amountMl = amountMl, timestamp = timestamp))
    }
}
class SavePreferences(private val repository: HydrationRepository, private val scheduler: ReminderScheduler) {
    suspend operator fun invoke(preferences: Preferences) {
        require(preferences.goalMl in 250..10000) { "ตั้งเป้าหมาย 250–10,000 มล." }
        require(preferences.hour in 0..23 && preferences.minute in 0..59)
        repository.savePreferences(preferences)
        scheduler.schedule(preferences)
    }
}
class DeleteDrink(private val repository: HydrationRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
fun weeklyTotals(entries: List<DrinkEntry>, today: LocalDate, zone: ZoneId): List<DailyTotal> {
    val totals = entries.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
    return (6 downTo 0).map { days ->
        val date = today.minusDays(days.toLong())
        DailyTotal(date, totals[date].orEmpty().sumOf { it.amountMl })
    }
}
fun progress(total: Int, goal: Int): Float = if (goal <= 0) 0f else (total.toFloat() / goal).coerceIn(0f, 1f)
fun nextReminder(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
    require(hour in 0..23 && minute in 0..59)
    val candidate = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
    return if (candidate.isAfter(now)) candidate else now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(now.zone)
}
