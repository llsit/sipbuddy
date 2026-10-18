package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.AuthRepository
class SignUpUseCase(private val repository: AuthRepository, private val validator: ValidateAuthInputUseCase) {
    suspend operator fun invoke(email: String, password: String): Boolean {
        val normalized = validator.email(email)
        validator.password(password, true)
        return repository.signUp(normalized, password)
    }
}
