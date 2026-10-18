package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.fake.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AuthUseCaseTest {
    private val repository = FakeAuthRepository()
    private val validator = ValidateAuthInputUseCase()
    @Test fun signInNormalizesEmailButPreservesPassword() = runTest {
        SignInUseCase(repository, validator)("  person@example.com  ", "  secret  ")
        assertEquals("person@example.com", repository.lastEmail)
        assertEquals("  secret  ", repository.lastPassword)
    }
    @Test fun invalidEmailNeverCallsNetwork() = runTest {
        try { SignInUseCase(repository, validator)("bad-email", "password"); fail() }
        catch (_: IllegalArgumentException) { assertEquals(0, repository.calls) }
    }
    @Test fun weakSignupPasswordNeverCallsNetwork() = runTest {
        try { SignUpUseCase(repository, validator)("a@b.com", "short"); fail() }
        catch (_: IllegalArgumentException) { assertEquals(0, repository.calls) }
    }
    @Test fun existingShortPasswordCanStillSignIn() = runTest {
        SignInUseCase(repository, validator)("a@b.com", "short")
        assertEquals(1, repository.calls)
    }
    @Test fun signupWaitsForEmailConfirmation() = runTest {
        assertFalse(SignUpUseCase(repository, validator)("a@b.com", "password"))
    }
    @Test fun resetNormalizesEmail() = runTest {
        SendPasswordResetUseCase(repository, validator)(" a@b.com ")
        assertEquals("a@b.com", repository.lastEmail)
    }
    @Test fun passwordUpdateRejectsWeakInput() = runTest {
        try { UpdatePasswordUseCase(repository, validator)("short"); fail() }
        catch (_: IllegalArgumentException) { assertEquals(0, repository.passwordUpdates) }
    }
}
