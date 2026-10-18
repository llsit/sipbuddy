package com.llsit.sipduddy.data.repository
import com.llsit.sipduddy.data.local.dao.DrinkDao
import com.llsit.sipduddy.data.mapper.DrinkEntryMapper
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.domain.repository.DrinkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class RoomDrinkRepository(private val dao: DrinkDao, private val mapper: DrinkEntryMapper,
    private val accounts: AccountRepository? = null) : DrinkRepository {
    private fun owner(): String = if (accounts == null) "" else checkNotNull(accounts.owner.value) { "กำลังตรวจบัญชี กรุณาลองใหม่" }
    private fun owned(entry: DrinkEntry, owner: String) {
        require(entry.ownerId == null || entry.ownerId == owner) { "บัญชีเปลี่ยนแล้ว กรุณาลองใหม่" }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEntries(startInclusive: Long,endExclusive: Long): Flow<List<DrinkEntry>> =
        (accounts?.owner ?: flowOf("")).filterNotNull().distinctUntilChanged().flatMapLatest { owner ->
            dao.observeEntries(startInclusive,endExclusive,owner).map { rows -> rows.map(mapper::toDomain) }
        }
    override suspend fun getTotalMl(startInclusive: Long,endExclusive: Long) = dao.getTotalMl(startInclusive,endExclusive,owner())
    override suspend fun add(entry: DrinkEntry) { val owner=owner(); owned(entry,owner); dao.insert(mapper.toEntity(entry).copy(ownerId=owner)) }
    override suspend fun delete(id: Long) = dao.delete(id,owner())
    override suspend fun getById(id: Long) = dao.getById(id,owner())?.let(mapper::toDomain)
    override suspend fun update(entry: DrinkEntry): Boolean { val owner=owner(); owned(entry,owner); return dao.editOwned(mapper.toEntity(entry).copy(ownerId=owner)) }
    override suspend fun deleteAndGet(id: Long) = dao.deleteAndGet(id,owner())?.let(mapper::toDomain)
    override suspend fun restore(entry: DrinkEntry): Boolean { val owner=owner(); owned(entry,owner); return dao.restore(mapper.toEntity(entry).copy(ownerId=owner)) != -1L }
}
