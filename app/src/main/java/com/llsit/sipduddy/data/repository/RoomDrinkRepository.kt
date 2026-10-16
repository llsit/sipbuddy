package com.llsit.sipduddy.data.repository

import com.llsit.sipduddy.data.local.dao.DrinkDao
import com.llsit.sipduddy.data.mapper.DrinkEntryMapper
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.repository.DrinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDrinkRepository(private val dao: DrinkDao, private val mapper: DrinkEntryMapper) :
    DrinkRepository {
    override fun observeEntries(startInclusive: Long, endExclusive: Long): Flow<List<DrinkEntry>> =
        dao.observeEntries(startInclusive, endExclusive).map { rows -> rows.map(mapper::toDomain) }

    override suspend fun add(entry: DrinkEntry) = dao.insert(mapper.toEntity(entry))
    override suspend fun delete(id: Long) = dao.delete(id)
}
