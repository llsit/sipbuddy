package com.llsit.sipduddy.presentation.formatter
import com.llsit.sipduddy.domain.model.AuthException
class AuthErrorFormatter {
    fun format(error: Exception): String = when (error) {
        is IllegalArgumentException, is AuthException -> error.message ?: "กรุณาตรวจข้อมูลอีกครั้ง"
        else -> "ทำรายการไม่สำเร็จ กรุณาลองใหม่"
    }
}
