package com.llsit.sipduddy.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.presentation.mapper.title

@Composable
internal fun DeleteDrinkDialog(
    entry: DrinkEntry,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ลบรายการนี้?") },
        text = { Text("${entry.drink.title} ${entry.amountMl} มล. จะถูกลบออกจากประวัติ") },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !busy) { Text("ลบรายการ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("เก็บไว้") } },
    )
}
