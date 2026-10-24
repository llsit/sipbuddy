package com.llsit.sipduddy.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.llsit.sipduddy.data.local.database.HydrationDatabase
import com.llsit.sipduddy.data.local.entity.SyncMetadataEntity
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.domain.usecase.SyncHydrationUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class HydrationSyncWorker(
    context: Context,
    parameters: WorkerParameters,
    private val sync: SyncHydrationUseCase,
    private val accounts: AccountRepository,
    private val database: HydrationDatabase,
    private val runtime: SyncRuntimeState,
    private val clock: Clock,
    private val settingsSync: com.llsit.sipduddy.domain.usecase.SyncHydrationSettingsUseCase,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString("owner")?.takeIf { it.isNotEmpty() } ?: return Result.success()
        val active = withTimeoutOrNull(30_000.milliseconds) { accounts.owner.filterNotNull().first() } ?: return Result.retry()
        if (active != owner) return Result.success()
        runtime.start(owner)
        return try {
            val result = sync(owner)
            val settingsResult = settingsSync(owner)
            val previous = database.sync().metadata(owner)
            database.sync().setMetadata(SyncMetadataEntity(owner, clock.millis(), conflicts = (previous?.conflicts ?: 0) + result.conflicts + settingsResult.conflicts))
            if (result.hasMorePending || settingsResult.hasMorePending) Result.retry() else Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (accounts.owner.value != owner) {
                Result.success()
            } else {
                val previous = database.sync().metadata(owner)
                database.sync().setMetadata(
                    SyncMetadataEntity(
                        owner,
                        previous?.lastSuccessAt,
                        "ซิงก์ยังไม่สำเร็จ ข้อมูลยังอยู่ในเครื่อง และจะลองใหม่เมื่อเชื่อมต่อได้",
                        previous?.conflicts ?: 0,
                    ),
                )
                Result.retry()
            }
        } finally {
            runtime.finish(owner)
        }
    }
}
