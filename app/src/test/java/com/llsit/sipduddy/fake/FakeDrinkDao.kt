package com.llsit.sipduddy.fake

import com.llsit.sipduddy.data.local.dao.DrinkDao
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDrinkDao : DrinkDao {
    val rows = MutableStateFlow<List<DrinkEntity>>(emptyList())
    override fun observeEntries(startInclusive: Long, endExclusive: Long) = rows.map { list ->
        list.filter { it.timestamp >= startInclusive && it.timestamp < endExclusive }
    }
    override suspend fun insert(entry: DrinkEntity) { rows.value = rows.value + entry.copy(id = 1) }
    override suspend fun delete(id: Long) { rows.value = rows.value.filterNot { it.id == id } }
}
