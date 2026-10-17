package com.llsit.sipduddy.fake

import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.repository.DrinkRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeDrinkRepository : DrinkRepository {
    val entries = MutableStateFlow<List<DrinkEntry>>(emptyList())
    var observationFailure: Exception? = null
    var writeFailure: Exception? = null
    var observationGate: CompletableDeferred<Unit>? = null
    var addGate: CompletableDeferred<Unit>? = null
    var addCalls = 0
    var totalCalls = 0
    var observationCalls = 0
    var lastStart = 0L
    var lastEnd = 0L
    override fun observeEntries(startInclusive: Long, endExclusive: Long) = flow {
        observationCalls++
        lastStart = startInclusive
        lastEnd = endExclusive
        observationFailure?.let { throw it }
        observationGate?.await()
        emitAll(entries.map { list -> list.filter { it.timestamp >= startInclusive && it.timestamp < endExclusive } })
    }
    override suspend fun getTotalMl(startInclusive: Long, endExclusive: Long): Long {
        totalCalls++
        observationFailure?.let { throw it }
        return entries.value.filter { it.timestamp >= startInclusive && it.timestamp < endExclusive }.sumOf { it.amountMl.toLong() }
    }
    override suspend fun add(entry: DrinkEntry) {
        addCalls++
        addGate?.await()
        writeFailure?.let { throw it }
        entries.value = entries.value + entry.copy(id = (entries.value.maxOfOrNull { it.id } ?: 0) + 1)
    }
    override suspend fun getById(id: Long) = entries.value.firstOrNull { it.id == id }
    override suspend fun update(entry: DrinkEntry): Boolean {
        writeFailure?.let { throw it }
        if (getById(entry.id) == null) return false
        entries.value = entries.value.map { if (it.id == entry.id) entry else it }
        return true
    }
    override suspend fun deleteAndGet(id: Long): DrinkEntry? {
        val entry = getById(id) ?: return null
        delete(id)
        return entry
    }
    override suspend fun restore(entry: DrinkEntry): Boolean {
        writeFailure?.let { throw it }
        if (getById(entry.id) != null) return false
        entries.value = entries.value + entry
        return true
    }
    override suspend fun delete(id: Long) {
        writeFailure?.let { throw it }
        entries.value = entries.value.filterNot { it.id == id }
    }
}
