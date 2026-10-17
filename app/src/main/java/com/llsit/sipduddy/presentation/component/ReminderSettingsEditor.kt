package com.llsit.sipduddy.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.llsit.sipduddy.domain.model.QuietHours
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.model.ReminderTime

@Composable
internal fun ReminderSettingsEditor(
    busy: Boolean,
    times: List<ReminderTime>,
    quiet: QuietHours,
    stopAtGoal: Boolean,
    onTimesChange: (List<ReminderTime>) -> Unit,
    onQuietChange: (QuietHours) -> Unit,
    onStopAtGoalChange: (Boolean) -> Unit,
) {
    var timeError by rememberSaveable { mutableStateOf<String?>(null) }
    times.forEachIndexed { index, time ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            ReminderTimeButton("เวลา", time, !busy) { updated ->
                if (updated != time && updated in times) {
                    timeError = "มีเวลาเตือนนี้แล้ว กรุณาเลือกเวลาอื่น"
                } else {
                    timeError = null
                    onTimesChange(times.toMutableList().also { it[index] = updated })
                }
            }
            TextButton(enabled = !busy && times.size > 1,
                onClick = { onTimesChange(times.filterIndexed { i, _ -> i != index }) }) { Text("ลบเวลา") }
        }
    }
    timeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    TextButton(enabled = !busy && times.size < ReminderSettings.MAX_TIMES, onClick = {
        val preferred = listOf(9, 12, 16, 20, 8, 11, 13, 15, 17, 19, 21, 7).map { it * 60 }
        val next = (preferred.asSequence() + (0..1439).asSequence()).map { ReminderTime(it / 60, it % 60) }
            .firstOrNull { it !in times && !quiet.contains(java.time.LocalTime.of(it.hour, it.minute)) }
        if (next != null) {
            timeError = null
            onTimesChange(times + next)
        } else {
            timeError = "ไม่มีเวลาว่างนอกช่วงพัก กรุณาปรับช่วงพักก่อน"
        }
    }) { Text("+ เพิ่มเวลาเตือน (สูงสุด 12 ช่วง)") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("พักการเตือนตอนกลางคืน")
        Switch(quiet.enabled, { onQuietChange(quiet.copy(enabled = it)) }, enabled = !busy)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ReminderTimeButton("เริ่ม", quiet.start, !busy && quiet.enabled) { onQuietChange(quiet.copy(start = it)) }
        ReminderTimeButton("ถึง", quiet.end, !busy && quiet.enabled) { onQuietChange(quiet.copy(end = it)) }
    }
    Text("เวลาเริ่มเท่ากับเวลาสิ้นสุด หมายถึงไม่พักการเตือน")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("หยุดเตือนเมื่อถึงเป้าของวันนี้", Modifier.weight(1f))
        Switch(stopAtGoal, onStopAtGoalChange, enabled = !busy)
    }
    Text("เริ่มเตือนใหม่ในวันถัดไป เวลาจริงอาจคลาดเคลื่อนตามการประหยัดแบตเตอรี่")
}
