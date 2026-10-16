package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DailyHydrationSummary
import com.llsit.sipduddy.domain.model.DateContext
import com.llsit.sipduddy.domain.model.DrinkEntry
import java.time.Instant

class CalculateWeeklyHydrationUseCase {
    operator fun invoke(
        entries: List<DrinkEntry>,
        context: DateContext
    ): List<DailyHydrationSummary> {
        val firstDate = context.date.minusDays(6)
        val currentDayEnd =
            context.date.plusDays(1).atStartOfDay(context.zone).toInstant().toEpochMilli()
        val firstDayStart = firstDate.atStartOfDay(context.zone).toInstant().toEpochMilli()
        val grouped = entries.asSequence()
            .filter { it.timestamp >= firstDayStart && it.timestamp < currentDayEnd }
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(context.zone).toLocalDate() }
        return (6 downTo 0).map { offset ->
            val date = context.date.minusDays(offset.toLong())
            DailyHydrationSummary(
                date, grouped[date].orEmpty().sortedWith(
                    compareByDescending<DrinkEntry> { it.timestamp }.thenByDescending { it.id },
                )
            )
        }
    }
}
