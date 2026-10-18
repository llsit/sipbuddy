package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.SyncOutcome
import com.llsit.sipduddy.domain.repository.*

class SyncHydrationSettingsUseCase(
    private val local: LocalSettingsSyncRepository,
    private val remote: RemoteSettingsSyncRepository,
    private val accounts: AccountRepository,
) {
    suspend operator fun invoke(owner: String): SyncOutcome {
        fun checkOwner() {
            check(owner.isNotEmpty() && accounts.owner.value == owner)
        }
        checkOwner()
        var conflicts = 0
        val sent = local.read(owner)
        if (sent.pending) {
            checkOwner()
            val result = remote.push(sent)
            checkOwner()
            check(result.record.ownerId == owner && result.record.serverVersion > 0)
            local.acknowledge(sent, result.record)
            if (!result.applied) conflicts++
        }
        checkOwner()
        val downloaded = remote.read(owner)
        checkOwner()
        if (downloaded != null) {
            check(downloaded.ownerId == owner && downloaded.serverVersion > 0)
            local.merge(downloaded)
        }
        return SyncOutcome(conflicts, local.read(owner).pending)
    }
}
