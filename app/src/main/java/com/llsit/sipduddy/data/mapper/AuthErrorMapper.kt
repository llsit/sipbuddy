package com.llsit.sipduddy.data.mapper

import io.github.jan.supabase.auth.exception.AuthRestException
import java.io.IOException

class AuthErrorMapper {
    fun format(error: Exception): String = when (error) {
        is IllegalArgumentException -> error.message ?: "กรุณาตรวจข้อมูลอีกครั้ง"

        is AuthRestException -> when (error.errorCode?.value) {
            "invalid_credentials" -> "อีเมลหรือรหัสผ่านไม่ถูกต้อง"
            "email_not_confirmed" -> "กรุณายืนยันอีเมลก่อนเข้าสู่ระบบ"
            "weak_password" -> "รหัสผ่านไม่ผ่านเงื่อนไข กรุณาใช้รหัสผ่านที่ยาวและคาดเดายากขึ้น"
            "over_email_send_rate_limit", "over_request_rate_limit" -> "ส่งคำขอบ่อยเกินไป กรุณารอสักครู่"
            "provider_disabled", "oauth_provider_not_supported" -> "ช่องทางเข้าสู่ระบบนี้ยังไม่พร้อมใช้งาน"
            else -> "ทำรายการไม่สำเร็จ กรุณาลองใหม่"
        }

        is IOException -> "เชื่อมต่อไม่ได้ กรุณาตรวจอินเทอร์เน็ตแล้วลองใหม่"

        else -> "ทำรายการไม่สำเร็จ กรุณาลองใหม่"
    }
}
