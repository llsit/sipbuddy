package com.llsit.sipduddy.domain.model

import java.time.LocalDate

data class DailyHydrationSummary(val date: LocalDate, val entries: List<DrinkEntry>) {
    val totalMl: Long = entries.sumOf { it.amountMl.toLong() }
}
