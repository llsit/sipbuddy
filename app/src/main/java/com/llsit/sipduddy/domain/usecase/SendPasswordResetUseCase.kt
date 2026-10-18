package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.AuthRepository
class SendPasswordResetUseCase(private val repository: AuthRepository, private val validator: ValidateAuthInputUseCase) {
    suspend operator fun invoke(email: String) = repository.sendPasswordReset(validator.email(email))
}
