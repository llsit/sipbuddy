package com.llsit.sipduddy.presentation.component
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.presentation.model.AuthUiState

@Composable
internal fun AccountCard(state: AuthUiState, onOpen: () -> Unit, onSignOut: () -> Unit) {
    SoftCard {
        Text("บัญชี SipBuddy", style = MaterialTheme.typography.titleMedium)
        val user = (state.session as? AuthSession.SignedIn)?.user
        Text(user?.email ?: if (state.session == AuthSession.Loading) "กำลังตรวจบัญชี…" else "ใช้งานบนเครื่องนี้แบบไม่เข้าสู่ระบบ")
        Text("ประวัติการดื่มเก็บเฉพาะในเครื่องนี้ ยังไม่มีการซิงก์หรือแยกประวัติตามบัญชี", style = MaterialTheme.typography.bodySmall)
        if (!state.isConfigured) Text("ระบบบัญชียังไม่พร้อมใช้งาน คุณยังบันทึกน้ำได้ตามปกติ", style = MaterialTheme.typography.bodySmall)
        if (user == null) Button(onClick = onOpen, enabled = state.isConfigured && !state.isBusy && state.session != AuthSession.Loading,
            modifier = Modifier.fillMaxWidth()) { Text("เข้าสู่ระบบ / สมัครสมาชิก") }
        else OutlinedButton(onClick = onSignOut, enabled = !state.isBusy) { Text(if (state.isBusy) "กำลังออกจากระบบ…" else "ออกจากระบบ") }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
