package com.llsit.sipduddy.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.llsit.sipduddy.data.auth.model.EncryptedSessionPayload
import com.llsit.sipduddy.data.local.preferences.AuthSessionPreferenceKeys
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class EncryptedDataStoreSessionManager(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SessionCipher,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SessionManager {
    private val json = Json {
        ignoreUnknownKeys = true
        // UserSession.expiresAt has a time-dependent default; always persist the original expiry.
        encodeDefaults = true
    }

    override suspend fun saveSession(session: UserSession): Unit = withContext(dispatcher) {
        val encrypted = cipher.encrypt(json.encodeToString(UserSession.serializer(), session))
        dataStore.edit { preferences ->
            // IV and ciphertext are committed together; a failed write cannot leave a mixed payload.
            preferences[AuthSessionPreferenceKeys.iv] = encrypted.iv
            preferences[AuthSessionPreferenceKeys.session] = encrypted.ciphertext
            preferences[AuthSessionPreferenceKeys.migrated] = true
        }
        Unit
    }

    override suspend fun loadSession(): UserSession = withContext(dispatcher) {
        val preferences = dataStore.data.first()
        val iv = preferences[AuthSessionPreferenceKeys.iv] ?: error("No saved session")
        val encrypted = preferences[AuthSessionPreferenceKeys.session] ?: error("No saved session")
        val raw = cipher.decrypt(EncryptedSessionPayload(iv, encrypted))
        json.decodeFromString(UserSession.serializer(), raw)
    }

    override suspend fun deleteSession(): Unit = withContext(dispatcher) {
        // edit also runs migration first, so signing out before the first read cannot resurrect legacy tokens.
        dataStore.edit { preferences ->
            preferences.remove(AuthSessionPreferenceKeys.iv)
            preferences.remove(AuthSessionPreferenceKeys.session)
            preferences[AuthSessionPreferenceKeys.migrated] = true
        }
        Unit
    }
}
