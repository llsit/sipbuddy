package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.AuthRepository
class SignInUseCase(private val repository: AuthRepository, private val validator: ValidateAuthInputUseCase) {
    suspend operator fun invoke(email: String, password: String) {
        val normalized = validator.email(email)
        validator.password(password, false)
        repository.signIn(normalized, password)
    }
}
