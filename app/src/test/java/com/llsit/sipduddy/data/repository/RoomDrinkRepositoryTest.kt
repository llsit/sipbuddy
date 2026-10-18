package com.llsit.sipduddy.data.repository

import com.llsit.sipduddy.data.mapper.DrinkEntryMapper
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.fake.FakeDrinkDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomDrinkRepositoryTest {
    @Test
    fun mapsAddsObservesAndDeletesThroughDao() = runBlocking {
        val dao = FakeDrinkDao()
        val repository = RoomDrinkRepository(dao, DrinkEntryMapper())
        repository.add(DrinkEntry(drink = DrinkType.MILK, amountMl = 150, timestamp = 100))
        assertEquals("MILK", dao.rows.value.single().drink)
        assertEquals(DrinkType.MILK, repository.observeEntries(100, 101).first().single().drink)
        assertTrue(repository.observeEntries(0, 100).first().isEmpty())
        repository.delete(1)
        assertTrue(repository.observeEntries(0, 200).first().isEmpty())
    }

    @Test fun editAndUndoMapExactIdsAndTimestampsAndDoNotOverwrite() = runBlocking {
        val dao = FakeDrinkDao()
        val repository = RoomDrinkRepository(dao, DrinkEntryMapper())
        val entry = DrinkEntry(8, DrinkType.JUICE, 200, 1234, ownerId = "")
        assertTrue(repository.restore(entry))
        val edited = entry.copy(amountMl = 500)
        assertTrue(repository.update(edited))
        assertEquals(edited, repository.deleteAndGet(8))
        assertEquals(null, repository.getById(8))
        assertTrue(repository.restore(edited))
        org.junit.Assert.assertFalse(repository.restore(entry))
        assertEquals(edited, repository.getById(8))
        org.junit.Assert.assertFalse(repository.update(entry.copy(id = 999)))
    }
}
