package com.llsit.sipduddy.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrinkDaoTest {
    private lateinit var database: HydrationDatabase
    @Before fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, HydrationDatabase::class.java).build()
    }
    @After fun closeDatabase() { database.close() }
    @Test fun usesInclusiveStartExclusiveEndAndDeletesOnlySelectedRow() = runBlocking {
        val dao = database.drinks()
        listOf(99L, 100L, 101L, 200L).forEach { timestamp -> dao.insert(DrinkEntity(drink = "WATER", amountMl = 250, timestamp = timestamp)) }
        val rows = dao.observeEntries(100, 200).first()
        assertEquals(listOf(101L, 100L), rows.map { it.timestamp })
        dao.delete(rows.first().id)
        assertEquals(listOf(100L), dao.observeEntries(100, 200).first().map { it.timestamp })
    }

    @Test fun editDeleteSnapshotAndRestoreUseStableIdentityWithoutOverwrite() = runBlocking {
        val dao = database.drinks()
        dao.insert(DrinkEntity(drink = "WATER", amountMl = 250, timestamp = 100))
        val original = dao.observeEntries(0, 200).first().single()
        val edited = original.copy(drink = "TEA", amountMl = 400)
        assertEquals(1, dao.update(edited))
        assertEquals(edited, dao.deleteAndGet(original.id))
        assertNull(dao.deleteAndGet(original.id))
        // A fresh entry after deletion must not reuse the deleted identity.
        dao.insert(DrinkEntity(drink = "MILK", amountMl = 150, timestamp = 101))
        assertNotEquals(original.id, dao.observeEntries(0, 200).first().single().id)
        assertEquals(edited.id, dao.restore(edited))
        assertEquals(-1L, dao.restore(original))
        assertEquals(edited, dao.getById(original.id))
        assertEquals(2, dao.observeEntries(0, 200).first().size)
        assertEquals(0, dao.update(edited.copy(id = 999)))
    }

    @Test fun aggregateReturnsZeroUsesExclusiveEndAndDoesNotOverflowInt() = runBlocking {
        val dao = database.drinks()
        assertEquals(0L, dao.getTotalMl(100, 200))
        listOf(99L, 100L, 101L, 200L).forEach { timestamp ->
            dao.insert(DrinkEntity(drink = "WATER", amountMl = Int.MAX_VALUE, timestamp = timestamp))
        }
        assertEquals(Int.MAX_VALUE.toLong() * 2, dao.getTotalMl(100, 200))
        assertEquals(0L, dao.getTotalMl(200, 200))
    }
}
