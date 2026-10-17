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
    override suspend fun getTotalMl(startInclusive: Long, endExclusive: Long) =
        rows.value.filter { it.timestamp >= startInclusive && it.timestamp < endExclusive }.sumOf { it.amountMl.toLong() }
    override suspend fun insert(entry: DrinkEntity) { rows.value = rows.value + entry.copy(id = 1) }
    override suspend fun getById(id: Long) = rows.value.firstOrNull { it.id == id }
    override suspend fun update(entry: DrinkEntity): Int {
        if (getById(entry.id) == null) return 0
        rows.value = rows.value.map { if (it.id == entry.id) entry else it }
        return 1
    }
    override suspend fun restore(entry: DrinkEntity): Long {
        if (getById(entry.id) != null) return -1L
        rows.value = rows.value + entry
        return entry.id
    }
    override suspend fun delete(id: Long) { rows.value = rows.value.filterNot { it.id == id } }
}
