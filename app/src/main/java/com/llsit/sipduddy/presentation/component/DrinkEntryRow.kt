package com.llsit.sipduddy.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onDelete: (DrinkEntry) -> Unit
) {
    Surface(color = Color.White.copy(alpha = .8f), shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = PaleBlue, shape = RoundedCornerShape(14.dp)) {
                Text(
                    entry.drink.symbol,
                    Modifier.padding(12.dp),
                    fontSize = 22.sp
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    entry.drink.title,
                    fontWeight = FontWeight.SemiBold
                ); Text(
                Instant.ofEpochMilli(entry.timestamp).atZone(zone)
                    .format(HydrationFormatters.time), fontSize = 12.sp
            )
            }
            Text("${entry.amountMl} มล.", fontWeight = FontWeight.Bold)
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
