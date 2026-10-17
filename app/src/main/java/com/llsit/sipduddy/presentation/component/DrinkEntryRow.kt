package com.llsit.sipduddy.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.llsit.sipduddy.presentation.animation.SipBuddyMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.presentation.formatter.HydrationFormatters
import com.llsit.sipduddy.presentation.mapper.symbol
import com.llsit.sipduddy.presentation.mapper.title
import com.llsit.sipduddy.ui.theme.PaleBlue
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun DrinkEntryRow(
    entry: DrinkEntry,
    zone: ZoneId,
    isSaving: Boolean,
    onEdit: (DrinkEntry) -> Unit,
    onDelete: (DrinkEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PaleBlue, shape = RoundedCornerShape(14.dp)) {
                Crossfade(entry.drink, animationSpec = tween(SipBuddyMotion.CONTENT_MILLIS), label = "drink icon") { drink ->
                    Text(drink.symbol, Modifier.padding(12.dp), fontSize = 22.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Crossfade(entry.drink, animationSpec = tween(SipBuddyMotion.CONTENT_MILLIS), label = "drink title") { drink ->
                    Text(drink.title, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    Instant.ofEpochMilli(entry.timestamp).atZone(zone)
                        .format(HydrationFormatters.time), fontSize = 12.sp
                )
            }
            AnimatedContent(
                targetState = entry.amountMl,
                transitionSpec = {
                    (fadeIn(tween(SipBuddyMotion.CONTENT_MILLIS)) + slideInVertically { it / 3 }) togetherWith
                        (fadeOut(tween(SipBuddyMotion.EXIT_MILLIS)) + slideOutVertically { -it / 3 })
                },
                label = "edited amount",
            ) { amount -> Text("$amount มล.", fontWeight = FontWeight.Bold) }
            Column {
                TextButton(onClick = { onEdit(entry) }, enabled = !isSaving, contentPadding = PaddingValues(4.dp)) {
                    Text("แก้ไข")
                }
                TextButton(onClick = { onDelete(entry) }, enabled = !isSaving, contentPadding = PaddingValues(4.dp)) {
                    Text("ลบ")
                }
            }
        }
    }
}
