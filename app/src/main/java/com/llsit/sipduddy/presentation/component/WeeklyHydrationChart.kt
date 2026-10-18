package com.llsit.sipduddy.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsit.sipduddy.domain.model.HydrationOverview
import com.llsit.sipduddy.presentation.formatter.HydrationFormatters
import com.llsit.sipduddy.ui.theme.Blue

@Composable
internal fun WeeklyHydrationChart(overview: HydrationOverview) {
    val maximum =
        maxOf(overview.settings.goalMl.toLong(), overview.days.maxOfOrNull { it.totalMl } ?: 0)
    SoftCard {
        Text("สัปดาห์นี้ ${overview.days.sumOf { it.totalMl }} มล.", fontWeight = FontWeight.Bold)
        Row(
            Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            overview.days.forEach { day ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("${day.totalMl}", fontSize = 10.sp, maxLines = 1)
                    Box(
                        Modifier
                            .width(18.dp)
                            .height((day.totalMl.toFloat() / maximum * 90).coerceAtLeast(4f).dp)
                            .background(
                                if (day.date == overview.dateContext.date) {
                                    Blue
                                } else {
                                    Color(
                                        0xFF9CD5E8,
                                    )
                                },
                                RoundedCornerShape(8.dp),
                            ),
                    )
                    Text(day.date.format(HydrationFormatters.weekday), fontSize = 11.sp)
                }
            }
        }
    }
}
