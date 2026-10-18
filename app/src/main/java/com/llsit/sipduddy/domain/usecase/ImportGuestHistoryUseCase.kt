package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.HydrationSyncRepository
class ImportGuestHistoryUseCase(private val repository: HydrationSyncRepository) { suspend operator fun invoke(expectedOwner: String) = repository.importGuestHistory(expectedOwner) }
