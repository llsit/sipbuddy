package com.llsit.sipduddy.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// Store refresh/access tokens encrypted with a non-exportable Android Keystore key.
class EncryptedSessionManager(context: Context, storageName: String = "auth_session") : SessionManager {
    private val preferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE)
    private val alias = "${context.packageName}.$storageName"
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build(),
            )
        }.generateKey()
    }
    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(json.encodeToString(UserSession.serializer(), session).toByteArray(Charsets.UTF_8))
        check(
            preferences.edit().putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString("session", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit(),
        )
    }
    override suspend fun loadSession(): UserSession = withContext(Dispatchers.IO) {
        val encrypted = preferences.getString("session", null) ?: error("No saved session")
        val iv = preferences.getString("iv", null) ?: error("No saved session")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        val raw = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)).toString(Charsets.UTF_8)
        json.decodeFromString(UserSession.serializer(), raw)
    }
    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        check(preferences.edit().clear().commit())
    }
}
