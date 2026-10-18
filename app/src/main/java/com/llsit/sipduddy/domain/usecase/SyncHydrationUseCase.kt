package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.SyncOutcome
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.domain.repository.LocalHydrationSyncRepository
import com.llsit.sipduddy.domain.repository.RemoteHydrationSyncRepository

class SyncHydrationUseCase(
    private val local: LocalHydrationSyncRepository,
    private val remote: RemoteHydrationSyncRepository,
    private val accounts: AccountRepository,
) {
    private fun checkOwner(owner: String) {
        check(owner.isNotEmpty() && accounts.owner.value == owner) { "Account changed" }
    }

    suspend operator fun invoke(owner: String): SyncOutcome {
        checkOwner(owner)
        var conflicts = 0
        // Bound execution time. Remaining mutations are retained and retried by WorkManager.
        var batches = 0
        while (batches++ < 10) {
            checkOwner(owner)
            val pending = local.pending(owner, 100)
            if (pending.isEmpty()) break
            for (sent in pending) {
                checkOwner(owner)
                check(sent.ownerId == owner)
                val result = remote.push(sent)
                checkOwner(owner)
                check(result.record.ownerId == owner && result.record.remoteId == sent.remoteId)
                local.acknowledge(sent, result.record)
                if (!result.applied) conflicts++
            }
        }
        var after: String? = null
        do {
            checkOwner(owner)
            val page = remote.page(owner, after, 200)
            checkOwner(owner)
            check(page.all { it.ownerId == owner })
            // UUID keyset pagination avoids offset drift; no timestamp cursor can skip late commits.
            check(page.zipWithNext().all { (a, b) -> a.remoteId < b.remoteId })
            after?.let { a ->
                check(page.all { it.remoteId > a })
            } ?: true

            local.merge(owner, page)
            if (page.isEmpty()) break
            after = page.last().remoteId
        } while (true)
        return SyncOutcome(conflicts, local.pending(owner, 1).isNotEmpty())
    }
}
