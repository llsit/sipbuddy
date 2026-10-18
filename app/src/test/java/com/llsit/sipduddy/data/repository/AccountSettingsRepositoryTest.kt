package com.llsit.sipduddy.data.repository

import androidx.datastore.preferences.core.*
import com.llsit.sipduddy.data.mapper.HydrationSettingsMapper
import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.fake.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AccountSettingsRepositoryTest {
    private val store = FakePreferencesDataStore()
    private val accounts = FakeAccountRepository("A")
    private val repo = DataStoreHydrationSettingsRepository(store, HydrationSettingsMapper(), accounts)
    @Test fun legacyGoalBelongsOnlyToGuestAndAccountsRemainSeparate() = runTest {
        store.data.value = preferencesOf(intPreferencesKey("goal_ml") to 3500)
        assertEquals(2000, repo.settings.first().goalMl)
        repo.saveForOwner(HydrationSettings(3000), "A")
        accounts.owner.value = "B"
        assertEquals(2000, repo.settings.first().goalMl)
        repo.saveForOwner(HydrationSettings(2500), "B")
        accounts.owner.value = ""
        assertEquals(3500, repo.settings.first().goalMl)
        accounts.owner.value = "A"
        assertEquals(3000, repo.settings.first().goalMl)
        assertEquals("A", repo.settings.first().ownerId)
    }
    @Test fun staleSaveCannotWriteNewAccount() = runTest {
        accounts.owner.value = "B"
        try { repo.saveForOwner(HydrationSettings(4000), "A"); fail() } catch (_: IllegalArgumentException) { }
        assertFalse(repo.read("A").pending); assertFalse(repo.read("B").pending)
    }
    @Test fun editDuringUploadSurvivesAndRebases() = runTest {
        repo.saveForOwner(HydrationSettings(3000), "A")
        val sent = repo.read("A")
        repo.saveForOwner(HydrationSettings(4000), "A")
        repo.acknowledge(sent, sent.copy(serverVersion = 1, pending = false))
        val current = repo.read("A")
        assertEquals(4000, current.settings.goalMl)
        assertTrue(current.pending); assertEquals(1L, current.serverVersion)
        assertNotEquals(sent.operationId, current.operationId)
        repo.merge(sent.copy(serverVersion = 2, pending = false))
        assertEquals(current, repo.read("A"))
    }
    @Test fun ackAndPullDoNotCreateNewMutationAndOldPullCannotOverwrite() = runTest {
        repo.saveForOwner(HydrationSettings(3000), "A")
        val sent = repo.read("A")
        repo.acknowledge(sent, sent.copy(serverVersion = 2, pending = false))
        assertFalse(repo.read("A").pending)
        repo.merge(sent.copy(settings = HydrationSettings(4500), serverVersion = 1, pending = false))
        assertEquals(3000, repo.read("A").settings.goalMl)
        repo.merge(sent.copy(settings = HydrationSettings(3500), serverVersion = 3, pending = false))
        assertEquals(3500, repo.read("A").settings.goalMl)
        assertEquals(sent.localVersion, repo.read("A").localVersion)
    }
    @Test fun offlineOutboxAndReminderDetailsSurviveRepositoryRestart() = runTest {
        val settings = HydrationSettings(2750, ReminderSettings(true,
            listOf(ReminderTime(9, 15), ReminderTime(16, 45)), QuietHours(true, ReminderTime(23, 0), ReminderTime(6, 30)), false))
        repo.saveForOwner(settings, "A")
        val restarted = DataStoreHydrationSettingsRepository(store, HydrationSettingsMapper(), accounts)
        assertEquals(settings, restarted.read("A").settings)
        assertEquals(repo.read("A"), restarted.read("A"))
        assertTrue(restarted.read("A").pending)
    }
    @Test fun guestWritesNeverEnterCloudOutbox() = runTest {
        accounts.owner.value = ""
        repo.saveForOwner(HydrationSettings(4500), "")
        assertFalse(repo.read("").pending)
        assertEquals(4500, repo.settings.first().goalMl)
    }
}
