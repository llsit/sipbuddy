package com.llsit.sipduddy.data.sync
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import com.llsit.sipduddy.data.mapper.SyncRecordMapper
import com.llsit.sipduddy.data.repository.RoomHydrationSyncRepository
import com.llsit.sipduddy.data.repository.DefaultHydrationSyncRepository
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.domain.repository.SyncRequestScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class RoomHydrationSyncTest {
    private lateinit var db: HydrationDatabase
    private lateinit var local: RoomHydrationSyncRepository
    private val accounts=object : AccountRepository { override val owner=MutableStateFlow<String?>("A") }
    private val scheduled=mutableListOf<String>()
    private val scheduler=object : SyncRequestScheduler {
        override fun request(owner: String,force: Boolean) { scheduled+=owner }
        override fun activate(owner: String) { }
    }
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,HydrationDatabase::class.java).build()
        local=RoomHydrationSyncRepository(db,SyncRecordMapper())
    }
    @After fun close() { db.close() }
    private suspend fun insert(owner: String="A"): DrinkEntity {
        db.drinks().insert(DrinkEntity(ownerId=owner,drink="WATER",amountMl=250,timestamp=100))
        return db.drinks().observeEntries(0,200,owner).first().single()
    }
    @Test fun editDuringUploadSurvivesAckAndUsesNewServerBase()=runBlocking {
        val row=insert(); val sent=local.pending("A",10).single()
        db.drinks().editOwned(row.copy(amountMl=500))
        local.acknowledge(sent,sent.copy(serverVersion=1))
        val pending=local.pending("A",10).single()
        assertEquals(500,pending.amountMl); assertEquals(1L,pending.serverVersion)
        assertNotEquals(sent.operationId,pending.operationId)
        local.acknowledge(pending,pending.copy(serverVersion=2))
        assertTrue(local.pending("A",10).isEmpty())
        assertEquals(500,db.drinks().getById(row.id,"A")?.amountMl)
    }
    @Test fun deleteDuringUploadIsNotLostAndUndoKeepsRemoteIdentity()=runBlocking {
        val row=insert();val sent=local.pending("A",10).single()
        val deleted=requireNotNull(db.drinks().deleteAndGet(row.id,"A"))
        local.acknowledge(sent,sent.copy(serverVersion=1))
        val tombstone=local.pending("A",10).single()
        assertTrue(tombstone.deleted);assertEquals(1L,tombstone.serverVersion)
        local.acknowledge(tombstone,tombstone.copy(serverVersion=2))
        assertTrue(db.drinks().observeEntries(0,200,"A").first().isEmpty())
        db.drinks().restore(deleted)
        val undo=local.pending("A",10).single()
        assertEquals(row.remoteId,undo.remoteId);assertEquals(2L,undo.serverVersion)
        assertTrue(undo.allowRestore);assertFalse(undo.deleted)
    }
    @Test fun pullsAreIdempotentDoNotOverwritePendingAndKeepTombstones()=runBlocking {
        val row=insert();val sent=local.pending("A",10).single()
        local.merge("A",listOf(sent.copy(amountMl=999,serverVersion=3)))
        assertEquals(250,db.drinks().getById(row.id,"A")?.amountMl)
        local.acknowledge(sent,sent.copy(serverVersion=1))
        val remote=sent.copy(remoteId=UUID.randomUUID().toString(),serverVersion=1,deleted=false)
        local.merge("A",listOf(remote));local.merge("A",listOf(remote))
        assertEquals(2,db.drinks().observeEntries(0,200,"A").first().size)
        local.merge("A",listOf(remote.copy(serverVersion=2,deleted=true)))
        assertEquals(1,db.drinks().observeEntries(0,200,"A").first().size)
        local.merge("A",listOf(remote))
        assertTrue(requireNotNull(db.sync().find("A",remote.remoteId)).deleted)
    }
    @Test fun wrongOwnerBatchCannotPartiallyMerge()=runBlocking {
        insert();val sent=local.pending("A",10).single()
        try { local.merge("B",listOf(sent)); fail() } catch(_: IllegalArgumentException) { }
        assertTrue(db.drinks().observeEntries(0,200,"B").first().isEmpty())
    }
    @Test fun guestImportRequiresCurrentAccountAndMovesOnlyGuestRecords()=runBlocking {
        val guest=insert("");insert("B")
        val repository=DefaultHydrationSyncRepository(db,accounts,scheduler,SyncRuntimeState(),
            com.llsit.sipduddy.data.repository.DataStoreHydrationSettingsRepository(
                androidx.datastore.preferences.core.PreferenceDataStoreFactory.create { java.io.File(
                    InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "sync-test-${UUID.randomUUID()}.preferences_pb") },
                com.llsit.sipduddy.data.mapper.HydrationSettingsMapper(),accounts))
        try { repository.importGuestHistory("B");fail() } catch(_: IllegalArgumentException) { }
        assertEquals(1,repository.importGuestHistory("A"))
        assertTrue(db.drinks().observeEntries(0,200,"").first().isEmpty())
        assertEquals(guest.remoteId,db.drinks().observeEntries(0,200,"A").first().single().remoteId)
        assertEquals(1,db.drinks().observeEntries(0,200,"B").first().size)
        assertEquals(listOf("A"),scheduled)
        assertEquals(0,repository.importGuestHistory("A"))
    }
    @Test fun editingAnOfflineUndoPreservesExplicitRestoreIntent()=runBlocking {
        val row=insert();val sent=local.pending("A",10).single()
        local.acknowledge(sent,sent.copy(serverVersion=1))
        val snapshot=requireNotNull(db.drinks().deleteAndGet(row.id,"A"))
        val tombstone=local.pending("A",10).single()
        local.acknowledge(tombstone,tombstone.copy(serverVersion=2))
        db.drinks().restore(snapshot)
        val undo=requireNotNull(db.drinks().getById(row.id,"A"))
        db.drinks().editOwned(undo.copy(amountMl=600))
        val editedUndo=local.pending("A",10).single()
        assertTrue(editedUndo.allowRestore);assertEquals(600,editedUndo.amountMl)
    }
}
