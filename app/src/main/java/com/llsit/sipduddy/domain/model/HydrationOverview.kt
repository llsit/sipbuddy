package com.llsit.sipduddy.domain.model

data class HydrationOverview(
    val dateContext: DateContext,
    val settings: HydrationSettings,
    val days: List<DailyHydrationSummary>,
) {
    val today: DailyHydrationSummary get() = days.last()
    val totalMl: Long get() = today.totalMl
    val remainingMl: Long get() = (settings.goalMl.toLong() - totalMl).coerceAtLeast(0)
    val progress: Float get() = (totalMl.toDouble() / settings.goalMl).coerceIn(0.0, 1.0).toFloat()
}
