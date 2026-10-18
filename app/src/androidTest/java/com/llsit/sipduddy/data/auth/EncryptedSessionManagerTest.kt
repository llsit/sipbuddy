package com.llsit.sipduddy.data.auth

import androidx.test.platform.app.InstrumentationRegistry
import io.github.jan.supabase.auth.user.UserSession
import java.security.KeyStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class EncryptedSessionManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storageName = "auth_session_test"
    private val preferences = context.getSharedPreferences(storageName, 0)
    private val manager = EncryptedSessionManager(context, storageName)

    @After fun cleanup() {
        preferences.edit().clear().commit()
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            deleteEntry("${context.packageName}.$storageName")
        }
    }

    @Test fun roundTripUsesEncryptionAndFreshIvAndDeletion() = runBlocking {
        val session = UserSession(
            accessToken = "synthetic-access-token",
            refreshToken = "synthetic-refresh-token",
            expiresIn = 3600,
            tokenType = "bearer",
        )
        manager.saveSession(session)
        assertEquals(session, manager.loadSession())
        val encrypted = preferences.getString("session", "")!!
        val firstIv = preferences.getString("iv", "")
        assertFalse(encrypted.contains("synthetic"))
        manager.saveSession(session)
        assertNotEquals(firstIv, preferences.getString("iv", ""))
        assertNotEquals(encrypted, preferences.getString("session", ""))
        assertEquals(session, EncryptedSessionManager(context, storageName).loadSession())
        manager.deleteSession()
        assertNull(manager.loadSessionOrNull())
        assertTrue(preferences.all.isEmpty())
    }
}
