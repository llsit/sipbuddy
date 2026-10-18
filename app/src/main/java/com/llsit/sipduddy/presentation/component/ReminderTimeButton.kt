package com.llsit.sipduddy.presentation.component

import android.app.TimePickerDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.llsit.sipduddy.domain.model.ReminderTime

@Composable
internal fun ReminderTimeButton(label: String, time: ReminderTime, enabled: Boolean, onChange: (ReminderTime) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(enabled = enabled, onClick = {
        TimePickerDialog(
            context,
            { _, hour, minute -> onChange(ReminderTime(hour, minute)) },
            time.hour,
            time.minute,
            true,
        ).show()
    }) { Text("$label %02d:%02d".format(time.hour, time.minute)) }
}
