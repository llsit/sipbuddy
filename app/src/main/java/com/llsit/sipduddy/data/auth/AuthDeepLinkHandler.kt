package com.llsit.sipduddy.data.auth

import android.content.Intent
import com.llsit.sipduddy.BuildConfig
import io.github.jan.supabase.auth.handleDeeplinks

class AuthDeepLinkHandler(private val provider: SupabaseClientProvider) {
    fun handle(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme != BuildConfig.AUTH_SCHEME || data.host != "auth-callback") return
        val recovery = data.getQueryParameter("mode") == "recovery"
        if (data.getQueryParameter("error") != null) {
            provider.callbackError.value = "ลิงก์เข้าสู่ระบบหมดอายุหรือถูกยกเลิก กรุณาลองใหม่"
            return
        }
        provider.client?.handleDeeplinks(
            intent,
            onSessionSuccess = { provider.recoveryRequired.value = recovery },
            onError = { provider.callbackError.value = "เปิดลิงก์ไม่สำเร็จ กรุณาขอลิงก์ใหม่และเปิดบนเครื่องเดิม" },
        )
        // Consume once so rotation does not exchange the same PKCE code again.
        intent.data = null
    }
}
