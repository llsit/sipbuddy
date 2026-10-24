package com.llsit.sipduddy.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.platform.app.InstrumentationRegistry
import com.llsit.sipduddy.data.local.preferences.AuthSessionDataStoreMigration
import com.llsit.sipduddy.data.local.preferences.AuthSessionPreferenceKeys as Keys
import io.github.jan.supabase.auth.user.UserSession
import java.io.IOException
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.junit.*
import org.junit.Assert.*

class EncryptedDataStoreSessionMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storageName = "auth_session_test_${UUID.randomUUID()}"
    private val file get() = context.preferencesDataStoreFile(storageName)
    private lateinit var job: CompletableJob
    private lateinit var dataStore: DataStore<Preferences>
    private val cipher = AndroidKeystoreSessionCipher(context, storageName)
    private val session = UserSession(accessToken = "synthetic-access", refreshToken = "synthetic-refresh", expiresIn = 3600, tokenType = "bearer", expiresAt = kotlin.time.Instant.parse("2030-01-01T00:00:00Z"))
    private val legacy get() = context.getSharedPreferences(storageName, 0)
    private fun manager() = EncryptedDataStoreSessionManager(dataStore, cipher)

    @Before fun setup() {
        check(context.packageName.endsWith(".qa")) { "Session migration tests must use the isolated QA package" }
        open()
    }
    private fun open(failMigration: Boolean = false) {
        job = SupervisorJob()
        val migrations = mutableListOf(AuthSessionDataStoreMigration.create(context, storageName))
        if (failMigration) {
            migrations += object : DataMigration<Preferences> {
                override suspend fun shouldMigrate(currentData: Preferences) = true
                override suspend fun migrate(currentData: Preferences): Preferences = throw IOException("synthetic migration failure")
                override suspend fun cleanUp() = Unit
            }
        }
        dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), migrations = migrations, produceFile = { file })
    }
    private suspend fun reopen(failMigration: Boolean = false) {
        job.cancelAndJoin()
        open(failMigration)
    }
    private fun seedLegacy(value: UserSession = session) {
        // Independent fixture reproduces the old manager's exact alias and encryption format.
        val alias = "${context.packageName}.$storageName"
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = (keyStore.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build(),
            )
        }.generateKey()
        val legacyCipher = Cipher.getInstance("AES/GCM/NoPadding")
        legacyCipher.init(Cipher.ENCRYPT_MODE, key)
        val raw = Json.encodeToString(UserSession.serializer(), value).toByteArray(Charsets.UTF_8)
        val encrypted = com.llsit.sipduddy.data.auth.model.EncryptedSessionPayload(
            Base64.encodeToString(legacyCipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(legacyCipher.doFinal(raw), Base64.NO_WRAP),
        )
        check(legacy.edit().putString("iv", encrypted.iv).putString("session", encrypted.ciphertext).putString("unrelated", "keep").commit())
    }

    @After fun cleanup(): Unit = runBlocking {
        if (!context.packageName.endsWith(".qa")) return@runBlocking
        if (::job.isInitialized) job.cancelAndJoin()
        context.deleteSharedPreferences(storageName)
        file.delete()
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            deleteEntry("${context.packageName}.$storageName")
        }
    }

    @Test fun keystoreRoundTripFreshIvReopenAndDelete() = runBlocking {
        val manager = manager()
        manager.saveSession(session)
        assertEquals(session, manager.loadSession())
        val first = dataStore.data.first()
        assertFalse(first[Keys.session]!!.contains("synthetic"))
        assertFalse(file.readBytes().toString(Charsets.UTF_8).contains("synthetic-access"))
        manager.saveSession(session)
        val next = dataStore.data.first()
        assertNotEquals(first[Keys.iv], next[Keys.iv])
        assertNotEquals(first[Keys.session], next[Keys.session])
        reopen()
        assertEquals(session, manager().loadSession())
        manager().deleteSession()
        reopen()
        assertNull(manager().loadSessionOrNull())
    }

    @Test fun migratesLegacyCiphertextUsingTheSameKeystoreAliasAndPreservesOtherKeys() = runBlocking {
        seedLegacy()
        val oldCiphertext = legacy.getString("session", null)
        assertEquals(session, manager().loadSession())
        assertEquals(oldCiphertext, dataStore.data.first()[Keys.session])
        assertFalse(legacy.contains("session"))
        assertFalse(legacy.contains("iv"))
        assertEquals("keep", legacy.getString("unrelated", null))
        reopen()
        assertEquals(session, manager().loadSession())
    }

    @Test fun logoutBeforeFirstReadCannotResurrectLegacySession() = runBlocking {
        seedLegacy()
        manager().deleteSession()
        assertNull(manager().loadSessionOrNull())
        assertFalse(legacy.contains("session"))
        assertFalse(legacy.contains("iv"))
        reopen()
        assertNull(manager().loadSessionOrNull())
    }

    @Test fun saveBeforeFirstReadReplacesLegacySessionAtomically() = runBlocking {
        seedLegacy()
        val newer = session.copy(accessToken = "synthetic-new")
        manager().saveSession(newer)
        assertFalse(legacy.contains("session"))
        reopen()
        assertEquals(newer, manager().loadSession())
    }

    @Test fun migrationNeverOverwritesAlreadyPersistedDataStoreSession() = runBlocking {
        val newer = session.copy(accessToken = "synthetic-new")
        manager().saveSession(newer)
        job.cancelAndJoin()
        seedLegacy()
        open()
        assertEquals(newer, manager().loadSession())
        assertFalse(legacy.contains("session"))
    }

    @Test fun migrationFailureLeavesLegacyDataAvailableForRetry() = runBlocking {
        seedLegacy()
        reopen(failMigration = true)
        try {
            manager().loadSession()
            fail()
        } catch (_: IOException) { }
        assertTrue(legacy.contains("session"))
        assertTrue(legacy.contains("iv"))
        reopen()
        assertEquals(session, manager().loadSession())
    }

    @Test fun staleLegacyFileAfterLogoutCannotRestoreOldIdentity() = runBlocking {
        manager().saveSession(session)
        manager().deleteSession()
        job.cancelAndJoin()
        seedLegacy()
        open()
        assertNull(manager().loadSessionOrNull())
        assertFalse(legacy.contains("session"))
        assertFalse(legacy.contains("iv"))
    }
}
