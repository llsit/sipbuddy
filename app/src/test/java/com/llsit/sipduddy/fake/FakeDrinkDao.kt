package com.llsit.sipduddy.fake
import com.llsit.sipduddy.data.local.dao.DrinkDao
import com.llsit.sipduddy.data.local.entity.DrinkEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
class FakeDrinkDao : DrinkDao {
    val rows = MutableStateFlow<List<DrinkEntity>>(emptyList())
    override fun observeEntries(startInclusive: Long, endExclusive: Long, owner: String) = rows.map { list ->
        list.filter { it.ownerId == owner && !it.deleted && it.timestamp >= startInclusive && it.timestamp < endExclusive }
    }
    override suspend fun getTotalMl(startInclusive: Long, endExclusive: Long, owner: String) = rows.value.filter { it.ownerId == owner && !it.deleted && it.timestamp >= startInclusive && it.timestamp < endExclusive }.sumOf { it.amountMl.toLong() }
    override suspend fun insert(entry: DrinkEntity) {
        rows.value = rows.value + entry.copy(id = if (entry.id == 0L) (rows.value.maxOfOrNull { it.id } ?: 0) + 1 else entry.id)
    }
    override suspend fun getById(id: Long, owner: String) = rows.value.firstOrNull { it.id == id && it.ownerId == owner && !it.deleted }
    override suspend fun getStored(id: Long, owner: String) = rows.value.firstOrNull { it.id == id && it.ownerId == owner }
    override suspend fun update(entry: DrinkEntity): Int {
        if (getStored(entry.id, entry.ownerId) == null) return 0
        rows.value = rows.value.map { if (it.id == entry.id) entry else it }
        return 1
    }
    override suspend fun markDeleted(id: Long, owner: String, operation: String) {
        rows.value = rows.value.map {
            if (it.id == id && it.ownerId == owner && !it.deleted) {
                it.copy(deleted = true, pending = true, operationId = operation, localVersion = it.localVersion + 1, allowRestore = false)
            } else {
                it
            }
        }
    }
}
