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
}
