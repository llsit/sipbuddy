package com.llsit.sipduddy.domain.repository

import com.llsit.sipduddy.domain.model.DrinkEntry
import kotlinx.coroutines.flow.Flow

interface DrinkRepository {
    fun observeEntries(startInclusive: Long, endExclusive: Long): Flow<List<DrinkEntry>>
    suspend fun getTotalMl(startInclusive: Long, endExclusive: Long): Long
    suspend fun add(entry: DrinkEntry)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): DrinkEntry?
    suspend fun update(entry: DrinkEntry): Boolean
    suspend fun deleteAndGet(id: Long): DrinkEntry?
    suspend fun restore(entry: DrinkEntry): Boolean
}
