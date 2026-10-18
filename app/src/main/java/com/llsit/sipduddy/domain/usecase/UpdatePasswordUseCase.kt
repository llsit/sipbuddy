package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.AuthRepository
class UpdatePasswordUseCase(private val repository: AuthRepository, private val validator: ValidateAuthInputUseCase) {
    suspend operator fun invoke(password: String) {
        validator.password(password, true)
        repository.updatePassword(password)
    }
}
