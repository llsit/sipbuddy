package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.AuthRepository
class ObserveAuthSessionUseCase(private val repository: AuthRepository) {
    val isConfigured get() = repository.isConfigured
    operator fun invoke() = repository.session
    fun callbackError() = repository.callbackError
    fun clearCallbackError() = repository.clearCallbackError()
    fun recoveryRequired() = repository.recoveryRequired
}
