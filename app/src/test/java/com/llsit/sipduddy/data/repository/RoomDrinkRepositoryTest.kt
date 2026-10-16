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
}
