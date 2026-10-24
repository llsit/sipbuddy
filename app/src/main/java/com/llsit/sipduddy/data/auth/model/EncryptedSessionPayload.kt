package com.llsit.sipduddy.data.auth.model

data class EncryptedSessionPayload(val iv: String, val ciphertext: String)
