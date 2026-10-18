package com.llsit.sipduddy.data.repository
import androidx.room.withTransaction
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.domain.repository.SyncRequestScheduler
import com.llsit.sipduddy.data.sync.SyncRuntimeState
import com.llsit.sipduddy.domain.model.SyncStatus
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.domain.repository.HydrationSyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.UUID
class DefaultHydrationSyncRepository(private val database: HydrationDatabase,private val accounts: AccountRepository,
    private val scheduler: SyncRequestScheduler,private val runtime: SyncRuntimeState,
    private val settingsSync: com.llsit.sipduddy.domain.repository.LocalSettingsSyncRepository) : HydrationSyncRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val status = accounts.owner.filterNotNull().flatMapLatest { owner ->
        combine(combine(database.sync().observePendingCount(owner),settingsSync.observeRecord(owner)) { count, settings ->
                count + if (settings.pending) 1 else 0
            },database.sync().observeGuestCount(),
            database.sync().observeMetadata(owner),runtime.running) { pending,guests,metadata,running ->
            SyncStatus(owner,if (owner.isEmpty()) 0 else pending,guests,owner in running,
                metadata?.lastSuccessAt,metadata?.error,metadata?.conflicts ?: 0)
        }
    }
    override fun requestSync() { accounts.owner.value?.takeIf { it.isNotEmpty() }?.let { scheduler.request(it,force=true) } }
    override suspend fun importGuestHistory(expectedOwner: String): Int {
        require(expectedOwner.isNotEmpty() && accounts.owner.value == expectedOwner) { "บัญชีเปลี่ยนแล้ว กรุณาลองใหม่" }
        val count=database.withTransaction {
            var count=0
            do {
                val guests=database.sync().guests()
                guests.forEach { row -> database.drinks().update(row.copy(ownerId=expectedOwner,pending=true,
                    serverVersion=0,localVersion=row.localVersion+1,operationId=UUID.randomUUID().toString())) }
                count+=guests.size
            } while (guests.size == 200)
            count
        }
        scheduler.request(expectedOwner)
        return count
    }
}
