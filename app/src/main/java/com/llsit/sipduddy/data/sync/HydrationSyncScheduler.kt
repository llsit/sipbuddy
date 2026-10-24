package com.llsit.sipduddy.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.llsit.sipduddy.domain.repository.SyncRequestScheduler
import java.util.concurrent.TimeUnit

class HydrationSyncScheduler(private val context: Context) : SyncRequestScheduler {
    private val manager get() = WorkManager.getInstance(context)
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    override fun request(owner: String, force: Boolean) {
        if (owner.isEmpty()) return
        val work = OneTimeWorkRequestBuilder<HydrationSyncWorker>().setConstraints(constraints)
            .setInputData(workDataOf("owner" to owner)).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        // Appending preserves a wakeup when a local write lands just as a running sync finishes.
        manager.enqueueUniqueWork("hydration-sync:$owner", if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }

    override fun activate(owner: String) {
        if (owner.isEmpty()) {
            manager.cancelUniqueWork("hydration-periodic-sync")
            return
        }
        val work = PeriodicWorkRequestBuilder<HydrationSyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints)
            .setInputData(workDataOf("owner" to owner)).build()
        manager.enqueueUniquePeriodicWork("hydration-periodic-sync", ExistingPeriodicWorkPolicy.UPDATE, work)
        request(owner)
    }
}
