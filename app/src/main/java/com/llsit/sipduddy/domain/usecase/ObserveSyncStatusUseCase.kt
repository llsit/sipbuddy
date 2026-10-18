package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.HydrationSyncRepository
class ObserveSyncStatusUseCase(private val repository: HydrationSyncRepository) {
    operator fun invoke() = repository.status
}
