package com.llsit.sipduddy.domain.usecase

class ValidateAuthInputUseCase {
    fun email(value: String): String {
        val email = value.trim()
        require(email.length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) {
            "กรุณาระบุอีเมลให้ถูกต้อง"
        }
        return email
    }
    fun password(value: String, isNew: Boolean) {
        require(value.isNotEmpty()) { "กรุณาระบุรหัสผ่าน" }
        require(!isNew || value.length >= 8) { "รหัสผ่านใหม่ต้องมีอย่างน้อย 8 ตัวอักษร" }
        require(value.length <= 128) { "รหัสผ่านยาวเกิน 128 ตัวอักษร" }
    }
}
