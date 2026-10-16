package com.llsit.sipduddy.domain.repository

import com.llsit.sipduddy.domain.model.DrinkEntry
import kotlinx.coroutines.flow.Flow

interface DrinkRepository {
    fun observeEntries(startInclusive: Long, endExclusive: Long): Flow<List<DrinkEntry>>
    suspend fun add(entry: DrinkEntry)
    suspend fun delete(id: Long)
}
