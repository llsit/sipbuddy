package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.domain.repository.*
import com.llsit.sipduddy.fake.FakeAccountRepository
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SyncHydrationUseCaseTest {
    private val accounts = FakeAccountRepository("account-A")
    private val record = SyncRecord("account-A", UUID.randomUUID().toString(), "WATER", 250, 100, false, 0, UUID.randomUUID().toString(), 1)
    private val queue = mutableListOf(record)
    private val acknowledgements = mutableListOf<SyncRecord>()
    private val downloaded = mutableListOf<SyncRecord>()
    private var afterPush: () -> Unit = {}
    private var failPush = false
    private var wrongOwner = false
    private var conflict = false
    private var pageCalls = 0
    private val local = object : LocalHydrationSyncRepository {
        override suspend fun pending(owner: String, limit: Int) = queue.take(limit)
        override suspend fun acknowledge(sent: SyncRecord, canonical: SyncRecord) {
            acknowledgements += canonical
            queue.remove(sent)
        }
        override suspend fun merge(owner: String, records: List<SyncRecord>) {
            downloaded += records
        }
    }
    private val remote = object : RemoteHydrationSyncRepository {
        override suspend fun push(record: SyncRecord): SyncPushResult {
            if (failPush) throw IOException("offline")
            afterPush()
            return SyncPushResult(!conflict, record.copy(serverVersion = 1, ownerId = if (wrongOwner) "account-B" else record.ownerId))
        }
        override suspend fun page(owner: String, afterId: String?, limit: Int): List<SyncRecord> {
            pageCalls++
            return emptyList()
        }
    }
    private fun sync() = SyncHydrationUseCase(local, remote, accounts)

    @Test fun pushesThenAcknowledgesAndPulls() = runTest {
        val result = sync()("account-A")
        assertFalse(result.hasMorePending)
        assertEquals(1, acknowledgements.size)
        assertEquals(1, pageCalls)
    }

    @Test fun networkFailureLeavesTheSameOperationPendingForIdempotentRetry() = runTest {
        failPush = true
        try {
            sync()("account-A")
            fail()
        } catch (_: IOException) { }
        assertEquals(record, queue.single())
        assertTrue(acknowledgements.isEmpty())
        failPush = false
        sync()("account-A")
        assertTrue(queue.isEmpty())
    }

    @Test fun switchingAccountDuringUploadDoesNotApplyAResponseToB() = runTest {
        afterPush = { accounts.owner.value = "account-B" }
        try {
            sync()("account-A")
            fail()
        } catch (_: IllegalStateException) { }
        assertTrue(acknowledgements.isEmpty())
        assertEquals(record, queue.single())
        assertEquals(0, pageCalls)
    }

    @Test fun foreignOwnerResponseIsRejected() = runTest {
        wrongOwner = true
        try {
            sync()("account-A")
            fail()
        } catch (_: IllegalStateException) { }
        assertTrue(acknowledgements.isEmpty())
    }

    @Test fun guestNeverCallsRemote() = runTest {
        accounts.owner.value = ""
        try {
            sync()("")
            fail()
        } catch (_: IllegalStateException) { }
        assertEquals(0, pageCalls)
        assertTrue(acknowledgements.isEmpty())
    }

    @Test fun serverConflictUsesCanonicalVersionAndReportsIt() = runTest {
        conflict = true
        assertEquals(1, sync()("account-A").conflicts)
        assertEquals(1L, acknowledgements.single().serverVersion)
    }

    @Test fun paginatesBeyondServerDefaultWithoutTimestampWatermark() = runTest {
        queue.clear()
        val rows = (1..450).map { record.copy(remoteId = "00000000-0000-0000-0000-%012d".format(it), serverVersion = 1) }
        val paging = object : RemoteHydrationSyncRepository {
            override suspend fun push(record: SyncRecord) = error("unexpected push")
            override suspend fun page(owner: String, afterId: String?, limit: Int) = rows.filter { afterId == null || it.remoteId > afterId }.take(limit)
        }
        SyncHydrationUseCase(local, paging, accounts)("account-A")
        assertEquals(450, downloaded.size)
        assertEquals(450, downloaded.map { it.remoteId }.toSet().size)
    }
}
