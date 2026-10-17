package com.llsit.sipduddy.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsit.sipduddy.domain.model.DateContext
import com.llsit.sipduddy.domain.model.HydrationOverview
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.usecase.CalculateWeeklyHydrationUseCase
import com.llsit.sipduddy.presentation.component.DrinkEntryRow
import com.llsit.sipduddy.presentation.component.EmptyState
import com.llsit.sipduddy.presentation.component.SoftCard
import com.llsit.sipduddy.presentation.component.WaterMascot
import com.llsit.sipduddy.presentation.formatter.HydrationFormatters
import com.llsit.sipduddy.ui.theme.Blue
import com.llsit.sipduddy.ui.theme.Ink
import com.llsit.sipduddy.ui.theme.SipDuddyTheme
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun TodayScreen(
    overview: HydrationOverview,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
    onEdit: (DrinkEntry) -> Unit,
    onDelete: (DrinkEntry) -> Unit
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "mascot", contentType = "header") {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("ค่อย ๆ จิบ ก็ถึงเป้าได้", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text(
                    overview.dateContext.date.format(HydrationFormatters.fullDate),
                    fontSize = 13.sp
                )
                WaterMascot(overview.progress, Modifier.size(230.dp))
                Text(
                    if (overview.progress >= 1f) "เย้! วันนี้ถึงเป้าแล้ว ♡" else "เติมความสดชื่นให้บัดดี้กัน",
                    fontWeight = FontWeight.Medium
                )
            }
        }
        item(key = "goal", contentType = "card") {
            SoftCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ดื่มไปแล้ววันนี้", fontSize = 13.sp)
                        Text("${overview.totalMl}", fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "/ ${overview.settings.goalMl} มล.",
                        Modifier.padding(bottom = 8.dp),
                        color = Ink.copy(alpha = .65f)
                    )
                }
                LinearProgressIndicator(
                    progress = { overview.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = Blue,
                    trackColor = Color.White
                )
                Text(
                    if (overview.remainingMl == 0L) "สำเร็จแล้ว ดูแลตัวเองดีมากเลย" else "อีก ${overview.remainingMl} มล. ถึงเป้าหมาย",
                    fontSize = 13.sp
                )
            }
        }
        item(key = "add", contentType = "button") {
            Button(
                onClick = onAdd,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("+ บันทึกเครื่องดื่ม", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
        item(key = "entries_title", contentType = "heading") {
            Text(
                "จิบเล็ก ๆ ของวันนี้",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        if (overview.today.entries.isEmpty()) item(
            key = "empty",
            contentType = "empty"
        ) { EmptyState("เริ่มต้นด้วยน้ำสักแก้ว", "แตะบันทึกเครื่องดื่ม เพื่อเติมหยดแรกของวัน") }
        items(overview.today.entries, key = { it.id }, contentType = { "drink_entry" }) { entry ->
            DrinkEntryRow(entry, overview.dateContext.zone, busy, onEdit, onDelete)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF9EF, widthDp = 393, heightDp = 850)
@Composable
private fun TodayScreenPreview() {
    val date = LocalDate.of(2026, 10, 17)
    val context = DateContext(date, ZoneId.of("Asia/Bangkok"))
    val entries = listOf(
        DrinkEntry(
            1,
            DrinkType.WATER,
            750,
            date.atTime(9, 0).atZone(context.zone).toInstant().toEpochMilli()
        )
    )
    SipDuddyTheme {
        TodayScreen(
            HydrationOverview(
                context,
                HydrationSettings(),
                CalculateWeeklyHydrationUseCase()(entries, context)
            ), false, onAdd = {}, onEdit = {}, onDelete = {})
    }
}
