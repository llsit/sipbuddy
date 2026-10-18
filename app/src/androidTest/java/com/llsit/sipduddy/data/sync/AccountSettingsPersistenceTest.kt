package com.llsit.sipduddy.data.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.llsit.sipduddy.data.mapper.HydrationSettingsMapper
import com.llsit.sipduddy.data.repository.DataStoreHydrationSettingsRepository
import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.domain.repository.AccountRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class AccountSettingsPersistenceTest {
    @Test fun diskOutboxSurvivesReopenAndRemainsAccountScoped() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val file = File(context.cacheDir, "settings-${UUID.randomUUID()}.preferences_pb")
        val accounts = object : AccountRepository {
            override val owner = MutableStateFlow<String?>("A")
        }
        var job = SupervisorJob()
        fun open() = DataStoreHydrationSettingsRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file }), HydrationSettingsMapper(), accounts)
        try {
            val repo = open()
            val settings = HydrationSettings(3250, ReminderSettings(true, listOf(ReminderTime(9, 15), ReminderTime(14, 45))))
            repo.saveForOwner(settings, "A")
            val saved = repo.read("A")
            job.cancelAndJoin()
            job = SupervisorJob()
            val reopened = open()
            assertEquals(saved, reopened.read("A"))
            assertTrue(reopened.read("A").pending)
            accounts.owner.value = "B"
            assertEquals(2000, reopened.settings.first().goalMl)
            assertFalse(reopened.read("B").pending)
            accounts.owner.value = "A"
            reopened.acknowledge(saved, saved.copy(serverVersion = 1, pending = false))
            assertEquals(3250, reopened.settings.first().goalMl)
            assertFalse(reopened.read("A").pending)
        } finally {
            job.cancelAndJoin()
            file.delete()
        }
    }
}
