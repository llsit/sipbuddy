package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.repository.AuthRepository
class SignInWithGoogleUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signInWithGoogle()
}
