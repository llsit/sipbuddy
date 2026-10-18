package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.HydrationSyncRepository
class RequestHydrationSyncUseCase(private val repository: HydrationSyncRepository) { operator fun invoke() = repository.requestSync() }
