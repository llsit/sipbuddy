package com.llsit.sipduddy.data.mapper

import com.llsit.sipduddy.data.local.entity.DrinkEntity
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import org.junit.Assert.assertEquals
import org.junit.Test

class DrinkEntryMapperTest {
    private val mapper = DrinkEntryMapper()
    @Test
    fun preservesExistingStoredNamesAndRoundTripsAllFields() {
        val entry = DrinkEntry(7, DrinkType.COFFEE, 150, 123L)
        assertEquals(entry, mapper.toDomain(mapper.toEntity(entry)))
        assertEquals("COFFEE", mapper.toEntity(entry).drink)
    }

    @Test
    fun unknownStoredTypeKeepsRecordVisibleAndDeletable() {
        val entry = mapper.toDomain(DrinkEntity(42, "FUTURE_TYPE", 250, 1L))
        assertEquals(DrinkType.OTHER, entry.drink)
        assertEquals(42L, entry.id)
        assertEquals(250, entry.amountMl)
    }
}
