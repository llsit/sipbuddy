package com.llsit.sipduddy.data.sync

import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.domain.repository.*
import com.llsit.sipduddy.domain.usecase.RestoreReminderUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class HydrationSyncCoordinator(
    private val accounts: AccountRepository,
    private val database: HydrationDatabase,
    private val scheduler: SyncRequestScheduler,
    private val scope: CoroutineScope,
    private val restoreReminder: RestoreReminderUseCase,
    private val settings: LocalSettingsSyncRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start() {
        scope.launch {
            accounts.owner.filterNotNull().distinctUntilChanged().collectLatest { owner ->
                scheduler.activate(owner)
                coroutineScope {
                    // Includes remote changes and guest settings; scheduling never grants notification permission.
                    launch {
                        settings.observeRecord(owner).map { it.settings.reminder }.distinctUntilChanged().collect {
                            try { restoreReminder() }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { /* Platform receiver retries the saved schedule. */ }
                        }
                    }
                    if (owner.isNotEmpty()) launch {
                        combine(database.sync().observeMutationVersion(owner),
                            settings.observeRecord(owner).map { it.localVersion }) { history, preferences -> history to preferences }
                            .distinctUntilChanged().debounce(500).collect { (history, preferences) ->
                                if (history > 0 || preferences > 0) scheduler.request(owner)
                            }
                    }
                }
            }
        }
    }
}
