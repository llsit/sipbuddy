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
    var addGate: CompletableDeferred<Unit>? = null
    var addCalls = 0
    var lastStart = 0L
    var lastEnd = 0L
    override fun observeEntries(startInclusive: Long, endExclusive: Long) = flow {
        lastStart = startInclusive
        lastEnd = endExclusive
        observationFailure?.let { throw it }
        emitAll(entries.map { list -> list.filter { it.timestamp >= startInclusive && it.timestamp < endExclusive } })
    }
    override suspend fun add(entry: DrinkEntry) {
        addCalls++
        addGate?.await()
        writeFailure?.let { throw it }
        entries.value = entries.value + entry.copy(id = (entries.value.maxOfOrNull { it.id } ?: 0) + 1)
    }
    override suspend fun delete(id: Long) {
        writeFailure?.let { throw it }
        entries.value = entries.value.filterNot { it.id == id }
    }
}
