package com.llsit.sipduddy.presentation.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.HydrationOverview
import com.llsit.sipduddy.presentation.animation.SipBuddyMotion
import com.llsit.sipduddy.presentation.component.DrinkEntryRow
import com.llsit.sipduddy.presentation.component.WeeklyHydrationChart
import com.llsit.sipduddy.presentation.formatter.HydrationFormatters
import com.llsit.sipduddy.ui.theme.Ink

@Composable
internal fun HistoryScreen(
    overview: HydrationOverview,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onEdit: (DrinkEntry) -> Unit,
    onDelete: (DrinkEntry) -> Unit,
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "title", contentType = "heading") {
            Text(
                "ทีละจิบ ทีละวัน",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        item(
            key = "subtitle",
            contentType = "text",
        ) { Text("ย้อนหลัง 7 วัน รวมวันนี้ • ปริมาณเครื่องดื่มจริง", fontSize = 13.sp) }
        item(key = "chart", contentType = "chart") { WeeklyHydrationChart(overview) }
        overview.days.asReversed().forEach { day ->
            item(key = "day_${day.date.toEpochDay()}", contentType = "day_heading") {
                Text(
                    "${day.date.format(HydrationFormatters.shortDate)} · ${day.totalMl} มล.",
                    fontWeight = FontWeight.Bold,
                )
            }
            if (day.entries.isEmpty()) {
                item(
                    key = "empty_${day.date.toEpochDay()}",
                    contentType = "empty",
                ) {
                    Text("ยังไม่มีรายการ", fontSize = 13.sp, color = Ink.copy(alpha = .5f))
                }
            }
            items(day.entries, key = { it.id }, contentType = { "drink_entry" }) { entry ->
                DrinkEntryRow(
                    entry,
                    overview.dateContext.zone,
                    busy,
                    onEdit,
                    onDelete,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(SipBuddyMotion.CONTENT_MILLIS),
                        placementSpec = tween(SipBuddyMotion.PLACEMENT_MILLIS),
                        fadeOutSpec = tween(SipBuddyMotion.EXIT_MILLIS),
                    ),
                )
            }
        }
    }
}
