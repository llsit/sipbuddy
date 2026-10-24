package com.llsit.sipduddy.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.llsit.sipduddy.data.auth.model.EncryptedSessionPayload
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeystoreSessionCipher(context: Context, storageName: String = "auth_session") : SessionCipher {
    // Keep the legacy alias exactly: migrated ciphertext must use its original non-exportable key.
    private val alias = "${context.packageName}.$storageName"

    private fun key(): SecretKey = synchronized(keyLock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build(),
            )
        }.generateKey()
    }

    override fun encrypt(plaintext: String): EncryptedSessionPayload {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedSessionPayload(
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP),
        )
    }

    override fun decrypt(payload: EncryptedSessionPayload): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(payload.iv, Base64.NO_WRAP)))
        return cipher.doFinal(Base64.decode(payload.ciphertext, Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private companion object {
        val keyLock = Any()
    }
}
