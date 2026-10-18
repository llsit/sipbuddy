package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.data.mapper.HydrationSettingsMapper
import com.llsit.sipduddy.data.repository.DataStoreHydrationSettingsRepository
import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.domain.repository.RemoteSettingsSyncRepository
import com.llsit.sipduddy.fake.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SyncHydrationSettingsUseCaseTest {
    private val accounts = FakeAccountRepository("A")
    private val local = DataStoreHydrationSettingsRepository(FakePreferencesDataStore(), HydrationSettingsMapper(), accounts)
    private var cloud: SettingsSyncRecord? = null
    private var afterPush: suspend () -> Unit = {}
    private var offline = false
    private var conflict = false
    private var wrongOwner = false
    private var pushes = 0
    private val remote = object : RemoteSettingsSyncRepository {
        override suspend fun push(record: SettingsSyncRecord): SettingsSyncPushResult {
            pushes++
            if (offline) throw IOException("offline")
            val result = record.copy(ownerId = if (wrongOwner) "B" else "A", serverVersion = 1,
                settings = if (conflict) HydrationSettings(2500) else record.settings, pending = false)
            cloud = result; afterPush()
            return SettingsSyncPushResult(!conflict, result)
        }
        override suspend fun read(owner: String) = cloud
    }
    private fun sync() = SyncHydrationSettingsUseCase(local, remote, accounts)
    @Test fun untouchedDefaultsDownloadExistingRemoteWithoutUploading() = runTest {
        cloud = SettingsSyncRecord("A", HydrationSettings(3500), 4)
        sync()("A")
        assertEquals(0, pushes); assertEquals(3500, local.read("A").settings.goalMl)
    }
    @Test fun offlineFailureRetainsSameOperationForRetry() = runTest {
        local.saveForOwner(HydrationSettings(3000), "A")
        val before = local.read("A"); offline = true
        try { sync()("A"); fail() } catch (_: IOException) { }
        assertEquals(before, local.read("A"))
        offline = false; sync()("A")
        assertFalse(local.read("A").pending)
    }
    @Test fun accountSwitchDuringNetworkDoesNotAcknowledgeOrOverwriteB() = runTest {
        local.saveForOwner(HydrationSettings(3000), "A")
        afterPush = { accounts.owner.value = "B" }
        try { sync()("A"); fail() } catch (_: IllegalStateException) { }
        assertTrue(local.read("A").pending); assertEquals(2000, local.read("B").settings.goalMl)
    }
    @Test fun newLocalEditDuringUploadRemainsPendingForNextRun() = runTest {
        local.saveForOwner(HydrationSettings(3000), "A")
        afterPush = { local.saveForOwner(HydrationSettings(4000), "A") }
        assertTrue(sync()("A").hasMorePending)
        assertEquals(4000, local.read("A").settings.goalMl)
        assertEquals(1L, local.read("A").serverVersion)
    }
    @Test fun staleOfflineSettingsUseCanonicalServerAndReportConflict() = runTest {
        local.saveForOwner(HydrationSettings(3000), "A"); conflict = true
        assertEquals(1, sync()("A").conflicts)
        assertEquals(2500, local.read("A").settings.goalMl); assertFalse(local.read("A").pending)
    }
    @Test fun wrongOwnerResponseIsRejected() = runTest {
        local.saveForOwner(HydrationSettings(3000), "A"); wrongOwner = true
        try { sync()("A"); fail() } catch (_: IllegalStateException) { }
        assertTrue(local.read("A").pending)
    }
}
