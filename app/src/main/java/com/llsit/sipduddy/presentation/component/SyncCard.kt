package com.llsit.sipduddy.presentation.component
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.llsit.sipduddy.presentation.model.SyncUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun SyncCard(state: SyncUiState, onSync: () -> Unit, onImport: (String) -> Unit) {
    val status = state.status
    var confirmOwner by rememberSaveable { mutableStateOf<String?>(null) }
    SoftCard {
        Text("ข้อมูลและการซิงก์", style = MaterialTheme.typography.titleMedium)
        if (status.ownerId.isEmpty()) {
            Text("ประวัติและการตั้งค่าผู้เยี่ยมชมเก็บเฉพาะในเครื่อง เข้าสู่ระบบเพื่อซิงก์ข้อมูลของบัญชี")
        } else {
            Text(
                when {
                    status.isRunning -> "กำลังซิงก์ข้อมูล…"
                    status.pending > 0 -> "มี ${status.pending} รายการรอซิงก์ ใช้งานออฟไลน์ต่อได้"
                    status.lastSuccessAt != null -> "ประวัติและการตั้งค่าซิงก์แล้ว"
                    else -> "เตรียมซิงก์ประวัติและการตั้งค่า"
                },
            )
            status.lastSuccessAt?.let { time ->
                val formatted = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time))
                Text("ซิงก์ล่าสุด $formatted", style = MaterialTheme.typography.bodySmall)
            }
            status.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (status.conflicts > 0) Text("บางรายการหรือการตั้งค่าเปลี่ยนจากอีกเครื่อง ระบบใช้เวอร์ชันบนบัญชีเพื่อป้องกันข้อมูลทับกัน", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onSync, enabled = !status.isRunning) { Text("ซิงก์ตอนนี้") }
            if (status.guestCount > 0) {
                TextButton(onClick = { confirmOwner = status.ownerId }, enabled = !state.importing) {
                    Text("ย้ายประวัติผู้เยี่ยมชม ${status.guestCount} รายการเข้าบัญชีนี้")
                }
            }
        }
        state.message?.let { Text(it) }
    }
    confirmOwner?.let { owner ->
        AlertDialog(
            onDismissRequest = { confirmOwner = null },
            title = { Text("ย้ายประวัติเข้าบัญชีนี้?") },
            text = { Text("ประวัติผู้เยี่ยมชมจะย้ายออกจากพื้นที่ผู้เยี่ยมชมและซิงก์เข้าบัญชีที่เข้าสู่ระบบอยู่ ตรวจบัญชีก่อนยืนยัน") },
            confirmButton = {
                TextButton(onClick = {
                    confirmOwner = null
                    onImport(owner)
                }) { Text("ย้ายประวัติ") }
            },
            dismissButton = { TextButton(onClick = { confirmOwner = null }) { Text("ยกเลิก") } },
        )
    }
}
