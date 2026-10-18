package com.llsit.sipduddy.presentation.screen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.llsit.sipduddy.presentation.model.AuthMode
import com.llsit.sipduddy.presentation.model.AuthUiState

@Composable
internal fun AuthScreen(state: AuthUiState, onMode: (AuthMode) -> Unit, onSubmit: (String, String) -> Unit,
    onGoogle: () -> Unit, onClose: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords deliberately never enter saved-instance-state or the ViewModel.
    var password by remember(state.mode) { mutableStateOf("") }
    var confirmation by remember(state.mode) { mutableStateOf("") }
    var visible by remember(state.mode) { mutableStateOf(false) }
    val title = when (state.mode) {
        AuthMode.SIGN_IN -> "ยินดีต้อนรับกลับ"
        AuthMode.SIGN_UP -> "สมัครสมาชิก SipBuddy"
        AuthMode.RESET -> "ลืมรหัสผ่าน"
        AuthMode.UPDATE_PASSWORD -> "ตั้งรหัสผ่านใหม่"
    }
    val newPassword = state.mode == AuthMode.SIGN_UP || state.mode == AuthMode.UPDATE_PASSWORD
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("เข้าถึงบัญชีของคุณ หรือใช้งานบันทึกน้ำแบบออฟไลน์ต่อได้", style = MaterialTheme.typography.bodyMedium)
        if (state.mode != AuthMode.UPDATE_PASSWORD) OutlinedTextField(email, { email = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("อีเมล") }, singleLine = true,
            enabled = !state.isBusy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        if (state.mode != AuthMode.RESET) {
            OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text(if (newPassword) "รหัสผ่าน (อย่างน้อย 8 ตัวอักษร)" else "รหัสผ่าน") },
                singleLine = true, enabled = !state.isBusy,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "ซ่อน" else "แสดง") } })
            if (newPassword) OutlinedTextField(confirmation, { confirmation = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("ยืนยันรหัสผ่าน") }, enabled = !state.isBusy, singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmation.isNotEmpty() && confirmation != password)
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it) }
        if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = { onSubmit(email, password); password = ""; confirmation = "" },
            modifier = Modifier.fillMaxWidth(), enabled = state.isConfigured && !state.isBusy &&
                (!newPassword || password.isNotEmpty() && password == confirmation)) { Text(when (state.mode) {
                    AuthMode.SIGN_IN -> "เข้าสู่ระบบ"
                    AuthMode.SIGN_UP -> "สมัครสมาชิก"
                    AuthMode.RESET -> "ส่งลิงก์รีเซ็ตรหัสผ่าน"
                    AuthMode.UPDATE_PASSWORD -> "บันทึกรหัสผ่านใหม่"
                }) }
        if (state.mode == AuthMode.SIGN_IN || state.mode == AuthMode.SIGN_UP) {
            OutlinedButton(onClick = onGoogle, modifier = Modifier.fillMaxWidth(), enabled = state.isConfigured && !state.isBusy) {
                Text("เข้าสู่ระบบด้วย Google")
            }
            TextButton(onClick = { onMode(if (state.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN) }, enabled = !state.isBusy) {
                Text(if (state.mode == AuthMode.SIGN_IN) "ยังไม่มีบัญชี? สมัครสมาชิก" else "มีบัญชีแล้ว? เข้าสู่ระบบ")
            }
        }
        if (state.mode == AuthMode.SIGN_IN) TextButton(onClick = { onMode(AuthMode.RESET) }, enabled = !state.isBusy) { Text("ลืมรหัสผ่าน") }
        if (state.mode == AuthMode.RESET) TextButton(onClick = { onMode(AuthMode.SIGN_IN) }, enabled = !state.isBusy) { Text("กลับไปเข้าสู่ระบบ") }
        TextButton(onClick = onClose, enabled = !state.isBusy) { Text("กลับไปบันทึกน้ำ") }
    }
}
