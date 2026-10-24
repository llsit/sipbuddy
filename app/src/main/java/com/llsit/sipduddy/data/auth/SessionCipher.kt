package com.llsit.sipduddy.data.auth

import com.llsit.sipduddy.data.auth.model.EncryptedSessionPayload

interface SessionCipher {
    fun encrypt(plaintext: String): EncryptedSessionPayload
    fun decrypt(payload: EncryptedSessionPayload): String
}
