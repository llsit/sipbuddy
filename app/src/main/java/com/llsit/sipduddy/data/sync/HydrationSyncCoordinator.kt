package com.llsit.sipduddy.data.sync
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.domain.repository.SyncRequestScheduler
import com.llsit.sipduddy.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class HydrationSyncCoordinator(private val accounts: AccountRepository,private val database: HydrationDatabase,
    private val scheduler: SyncRequestScheduler,private val scope: CoroutineScope,
    private val restoreReminder: com.llsit.sipduddy.domain.usecase.RestoreReminderUseCase) {
    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    fun start() {
        scope.launch {
            accounts.owner.filterNotNull().distinctUntilChanged().collectLatest { owner ->
                scheduler.activate(owner)
                try { restoreReminder() }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { /* Saved reminder settings can be retried by the platform receiver. */ }
                if (owner.isNotEmpty()) database.sync().observeMutationVersion(owner).distinctUntilChanged().debounce(500).collect { stamp ->
                    if (stamp > 0) scheduler.request(owner)
                }
            }
        }
    }
}
