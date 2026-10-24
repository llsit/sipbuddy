package com.llsit.sipduddy.fake

import com.llsit.sipduddy.data.auth.SessionCipher
import com.llsit.sipduddy.data.auth.model.EncryptedSessionPayload
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// JVM-only test cipher. Production uses a non-exportable Android Keystore key instead.
class FakeSessionCipher : SessionCipher {
    private val key = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")
    override fun encrypt(plaintext: String): EncryptedSessionPayload {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedSessionPayload(Base64.getEncoder().encodeToString(cipher.iv), Base64.getEncoder().encodeToString(encrypted))
    }
    override fun decrypt(payload: EncryptedSessionPayload): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(payload.iv)))
        return cipher.doFinal(Base64.getDecoder().decode(payload.ciphertext)).toString(Charsets.UTF_8)
    }
}
