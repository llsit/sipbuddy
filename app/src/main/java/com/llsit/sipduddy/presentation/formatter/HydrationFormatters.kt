package com.llsit.sipduddy.presentation.formatter

import java.time.format.DateTimeFormatter
import java.util.Locale

internal object HydrationFormatters {
    private val thai = Locale.forLanguageTag("th")
    val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", thai)
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", thai)
    val weekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EE", thai)
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", thai)
}
