package com.llsit.sipduddy.presentation.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.QuietHours
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.model.ReminderTime
import com.llsit.sipduddy.presentation.component.EmptyState
import com.llsit.sipduddy.presentation.component.ReminderSettingsEditor
import com.llsit.sipduddy.presentation.component.SoftCard
import com.llsit.sipduddy.presentation.notification.NotificationPermissionController

@Composable
internal fun SettingsScreen(
    settings: HydrationSettings,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onSave: (HydrationSettings) -> Unit,
    accountContent: @Composable () -> Unit = {},
) {
    val prefs = settings
    var goal by rememberSaveable(prefs.goalMl) { mutableStateOf(prefs.goalMl.toString()) }
    val savedTimes = prefs.reminder.times.joinToString(",") { "${it.hour * 60 + it.minute}" }
    var timesValue by rememberSaveable(savedTimes) { mutableStateOf(savedTimes) }
    val times = timesValue.split(',').map { it.toInt() }.map { ReminderTime(it / 60, it % 60) }
    var quietEnabled by rememberSaveable(prefs.reminder.quietHours.enabled) { mutableStateOf(prefs.reminder.quietHours.enabled) }
    val savedStart = prefs.reminder.quietHours.start.hour * 60 + prefs.reminder.quietHours.start.minute
    val savedEnd = prefs.reminder.quietHours.end.hour * 60 + prefs.reminder.quietHours.end.minute
    var quietStart by rememberSaveable(savedStart) { mutableIntStateOf(savedStart) }
    var quietEnd by rememberSaveable(savedEnd) { mutableIntStateOf(savedEnd) }
    var stopAtGoal by rememberSaveable(prefs.reminder.stopAtGoal) { mutableStateOf(prefs.reminder.stopAtGoal) }
    val quiet = QuietHours(quietEnabled, ReminderTime(quietStart / 60, quietStart % 60), ReminderTime(quietEnd / 60, quietEnd % 60))
    var enabled by rememberSaveable(prefs.reminder.enabled) { mutableStateOf(prefs.reminder.enabled) }
    val context = LocalContext.current
    val permissionController = remember(context) { NotificationPermissionController(context) }

    var permissionGranted by remember { mutableStateOf(permissionController.isGranted()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = permissionController.isGranted()
    }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted = granted && permissionController.isGranted()
        }
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        accountContent()
        Text("ดูแลตัวเองในแบบคุณ", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        SoftCard {
            Text("เป้าหมายรายวัน", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                enabled = !busy,
                value = goal,
                onValueChange = { goal = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("มิลลิลิตรต่อวัน") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = goal.toIntOrNull() !in HydrationSettings.MIN_GOAL_ML..HydrationSettings.MAX_GOAL_ML,
                supportingText = { Text("ปรับได้ตั้งแต่ 250–10,000 มล.") },
            )
            Text("เลือกเป้าที่เหมาะกับตัวคุณ", fontSize = 12.sp)
        }
        SoftCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("เตือนให้จิบน้ำ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Switch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !busy)
            }
            ReminderSettingsEditor(
                busy,
                times,
                quiet,
                stopAtGoal,
                onTimesChange = { list -> timesValue = list.joinToString(",") { "${it.hour * 60 + it.minute}" } },
                onQuietChange = {
                    quietEnabled = it.enabled
                    quietStart = it.start.hour * 60 + it.start.minute
                    quietEnd = it.end.hour * 60 + it.end.minute
                },
                onStopAtGoalChange = { stopAtGoal = it },
            )
            if (enabled && times.none { !quiet.contains(java.time.LocalTime.of(it.hour, it.minute)) }) {
                Text("เลือกเวลาเตือนอย่างน้อยหนึ่งช่วงนอกช่วงพัก", color = MaterialTheme.colorScheme.error)
            }
            if (!permissionGranted) {
                Text(
                    "ยังไม่ได้เปิดสิทธิ์แจ้งเตือน",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        request.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        permissionController.openSystemSettings()
                    }
                }) { Text("ขอสิทธิ์แจ้งเตือน") }
            }
            TextButton(onClick = {
                permissionController.openSystemSettings()
            }) { Text("เปิดการตั้งค่าแจ้งเตือนของระบบ") }
        }
        Button(
            onClick = {
                goal.toIntOrNull()?.let {
                    onSave(
                        HydrationSettings(
                            it,
                            ReminderSettings(enabled, times.sortedBy { it.hour * 60 + it.minute }, quiet, stopAtGoal),
                        ),
                    )
                }
            },
            enabled = !busy && goal.toIntOrNull() in HydrationSettings.MIN_GOAL_ML..HydrationSettings.MAX_GOAL_ML &&
                (!enabled || times.any { !quiet.contains(java.time.LocalTime.of(it.hour, it.minute)) }),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) { Text(if (busy) "กำลังบันทึก…" else "บันทึกการตั้งค่า") }
        EmptyState("พื้นที่เล็ก ๆ ของคุณ", "ข้อมูลอยู่ในเครื่อง ใช้ได้ออฟไลน์ ไม่ต้องเข้าสู่ระบบ")
    }
}
