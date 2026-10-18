package com.llsit.sipduddy.data.repository
import com.llsit.sipduddy.data.mapper.DrinkEntryMapper
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.fake.FakeAccountRepository
import com.llsit.sipduddy.fake.FakeDrinkDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class AccountIsolationTest {
    private val accounts = FakeAccountRepository("A")
    private val dao = FakeDrinkDao()
    private val repository = RoomDrinkRepository(dao, DrinkEntryMapper(), accounts)

    @Test fun offlineHistoryTotalsAndLookupAreScopedToAccount() = runTest {
        repository.add(DrinkEntry(drink = DrinkType.WATER, amountMl = 250, timestamp = 100, ownerId = "A"))
        val a = repository.observeEntries(0, 200).first().single()
        accounts.owner.value = "B"
        assertTrue(repository.observeEntries(0, 200).first().isEmpty())
        assertNull(repository.getById(a.id))
        assertEquals(0L, repository.getTotalMl(0, 200))
        repository.delete(a.id)
        repository.add(DrinkEntry(drink = DrinkType.MILK, amountMl = 500, timestamp = 100, ownerId = "B"))
        accounts.owner.value = "A"
        assertEquals(250L, repository.getTotalMl(0, 200))
        assertEquals(a, repository.getById(a.id))
    }

    @Test fun staleUndoEditAndQueuedAddCannotWriteToAnotherAccount() = runTest {
        repository.add(DrinkEntry(drink = DrinkType.WATER, amountMl = 250, timestamp = 100, ownerId = "A"))
        val deleted = requireNotNull(repository.deleteAndGet(repository.observeEntries(0, 200).first().single().id))
        accounts.owner.value = "B"
        try {
            repository.restore(deleted)
            fail()
        } catch (_: IllegalArgumentException) { }
        try {
            repository.update(deleted)
            fail()
        } catch (_: IllegalArgumentException) { }
        try {
            repository.add(deleted.copy(id = 0))
            fail()
        } catch (_: IllegalArgumentException) { }
        assertTrue(repository.observeEntries(0, 200).first().isEmpty())
    }

    @Test fun deleteAndUndoKeepUuidAndLeavePendingTombstone() = runTest {
        repository.add(DrinkEntry(drink = DrinkType.WATER, amountMl = 250, timestamp = 100, ownerId = "A"))
        val original = dao.rows.value.single()
        val snapshot = requireNotNull(repository.deleteAndGet(original.id))
        assertTrue(dao.rows.value.single().deleted)
        assertTrue(dao.rows.value.single().pending)
        assertTrue(repository.restore(snapshot))
        assertEquals(original.remoteId, dao.rows.value.single().remoteId)
        assertFalse(dao.rows.value.single().deleted)
        assertTrue(dao.rows.value.single().allowRestore)
    }
}
