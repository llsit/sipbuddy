package com.llsit.sipduddy.data.auth

import androidx.datastore.preferences.core.*
import com.llsit.sipduddy.data.local.preferences.AuthSessionPreferenceKeys as Keys
import com.llsit.sipduddy.fake.FakePreferencesDataStore
import com.llsit.sipduddy.fake.FakeSessionCipher
import io.github.jan.supabase.auth.user.UserSession
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedDataStoreSessionManagerTest {
    private val store = FakePreferencesDataStore()
    private val cipher = FakeSessionCipher()
    private val session = UserSession(accessToken = "synthetic-access", refreshToken = "synthetic-refresh", expiresIn = 3600, tokenType = "bearer")
    private fun TestScope.manager() = EncryptedDataStoreSessionManager(store, cipher, StandardTestDispatcher(testScheduler))

    @Test fun roundTripPersistsEncryptedPayloadWithFreshIv() = runTest {
        val manager = manager()
        manager.saveSession(session)
        val first = store.data.value
        assertEquals(session, manager.loadSession())
        assertFalse(first[Keys.session]!!.contains("synthetic"))
        manager.saveSession(session)
        assertNotEquals(first[Keys.iv], store.data.value[Keys.iv])
        assertNotEquals(first[Keys.session], store.data.value[Keys.session])
        assertEquals(session, manager().loadSession())
    }

    @Test fun failedWriteKeepsCompleteOldPayload() = runTest {
        val manager = manager()
        manager.saveSession(session)
        val before = store.data.value
        store.failure = IOException("disk full")
        try {
            manager.saveSession(session.copy(accessToken = "new"))
            fail()
        } catch (_: IOException) { }
        assertEquals(before, store.data.value)
        store.failure = null
        assertEquals(session, manager.loadSession())
    }

    @Test fun deleteRemovesBothKeysAndPreservesOtherPreferences() = runTest {
        val other = stringPreferencesKey("future_metadata")
        store.data.value = preferencesOf(other to "keep")
        val manager = manager()
        manager.saveSession(session)
        manager.deleteSession()
        assertNull(store.data.value[Keys.iv])
        assertNull(store.data.value[Keys.session])
        assertEquals(true, store.data.value[Keys.migrated])
        assertEquals("keep", store.data.value[other])
        assertNull(manager().loadSessionOrNull())
        manager.deleteSession()
        assertNull(manager.loadSessionOrNull())
    }

    @Test fun failedDeleteDoesNotPartiallyRemoveSession() = runTest {
        val manager = manager()
        manager.saveSession(session)
        val before = store.data.value
        store.failure = IOException("disk full")
        try {
            manager.deleteSession()
            fail()
        } catch (_: IOException) { }
        assertEquals(before, store.data.value)
    }

    @Test fun missingIvOrCiphertextCannotLoadPartialSession() = runTest {
        val manager = manager()
        store.data.value = preferencesOf(Keys.iv to "only-iv")
        assertNull(manager.loadSessionOrNull())
        store.data.value = preferencesOf(Keys.session to "only-ciphertext")
        assertNull(manager.loadSessionOrNull())
    }

    @Test fun tamperedCiphertextIsRejectedWithoutPlaintextFallbackOrDeletingEvidence() = runTest {
        val manager = manager()
        manager.saveSession(session)
        store.data.value = store.data.value.toMutablePreferences().apply { this[Keys.session] = "AAAA" }
        val corrupted = store.data.value
        assertNull(manager.loadSessionOrNull())
        assertEquals(corrupted, store.data.value)
    }

    @Test fun cancellationPropagatesAndKeepsPreviousPayload() = runTest {
        val manager = manager()
        manager.saveSession(session)
        val before = store.data.value
        store.failure = CancellationException("cancelled")
        try {
            manager.saveSession(session.copy(accessToken = "new"))
            fail()
        } catch (_: CancellationException) { }
        assertEquals(before, store.data.value)
    }

    @Test fun concurrentSavesNeverMixIvAndCiphertext() = runTest {
        val manager = manager()
        val sessions = (1..20).map { session.copy(accessToken = "synthetic-$it") }
        sessions.map { value -> async { manager.saveSession(value) } }.awaitAll()
        assertTrue(manager.loadSession() in sessions)
    }

    @Test fun expiryIsSerializedExplicitlyInsteadOfBeingRecomputedOnDecode() = runTest {
        val manager = manager()
        manager.saveSession(session)
        val saved = store.data.value
        val payload = com.llsit.sipduddy.data.auth.model.EncryptedSessionPayload(saved[Keys.iv]!!, saved[Keys.session]!!)
        val raw = cipher.decrypt(payload)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(raw) as kotlinx.serialization.json.JsonObject
        assertTrue(json.containsKey("expiresAt"))
        assertEquals(session.expiresAt, manager().loadSession().expiresAt)
    }
}
