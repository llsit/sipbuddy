package com.llsit.sipduddy.presentation
import androidx.lifecycle.ViewModelStore
import com.llsit.sipduddy.domain.model.AuthException
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.domain.usecase.*
import com.llsit.sipduddy.fake.FakeAuthRepository
import com.llsit.sipduddy.presentation.formatter.AuthErrorFormatter
import com.llsit.sipduddy.presentation.model.AuthMode
import com.llsit.sipduddy.rule.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val repository = FakeAuthRepository()
    private val store = ViewModelStore()
    private fun viewModel(): AuthViewModel {
        val validator = ValidateAuthInputUseCase()
        return AuthViewModel(
            ObserveAuthSessionUseCase(repository),
            SignInUseCase(repository, validator),
            SignUpUseCase(repository, validator),
            SignInWithGoogleUseCase(repository),
            SignOutUseCase(repository),
            SendPasswordResetUseCase(repository, validator),
            UpdatePasswordUseCase(repository, validator),
            AuthErrorFormatter(),
        )
            .also { store.put("auth", it) }
    }

    @After fun clear() {
        store.clear()
    }

    @Test fun repeatedSubmitOnlyStartsOneRequest() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.open()
        repository.gate = CompletableDeferred()
        vm.submit("a@b.com", "password")
        vm.submit("a@b.com", "password")
        runCurrent()
        assertEquals(1, repository.calls)
        assertTrue(vm.state.value.isBusy)
        vm.mode(AuthMode.SIGN_UP)
        assertEquals(AuthMode.SIGN_IN, vm.state.value.mode)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.isBusy)
        assertFalse(vm.state.value.showForm)
        assertTrue(vm.state.value.session is AuthSession.SignedIn)
    }

    @Test fun signupWithoutSessionKeepsConfirmationMessageVisible() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.open()
        vm.mode(AuthMode.SIGN_UP)
        vm.submit("a@b.com", "password")
        runCurrent()
        assertTrue(vm.state.value.showForm)
        assertNotNull(vm.state.value.message)
        assertEquals(AuthSession.SignedOut, vm.state.value.session)
    }

    @Test fun failureKeepsFormAndAllowsRetry() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.open()
        repository.failure = AuthException("ตรวจอินเทอร์เน็ต")
        vm.submit("a@b.com", "password")
        runCurrent()
        assertEquals("ตรวจอินเทอร์เน็ต", vm.state.value.error)
        assertFalse(vm.state.value.isBusy)
        repository.failure = null
        vm.submit("a@b.com", "password")
        runCurrent()
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.showForm)
    }

    @Test fun googleLaunchDoesNotPretendUserIsAuthenticated() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.open()
        vm.google()
        runCurrent()
        assertEquals(1, repository.googleCalls)
        assertEquals(AuthSession.SignedOut, vm.state.value.session)
        assertTrue(vm.state.value.showForm)
        assertFalse(vm.state.value.isBusy)
    }

    @Test fun recoveryCallbackOpensNewPasswordForm() = runTest {
        val vm = viewModel()
        runCurrent()
        repository.recoveryRequired.value = true
        runCurrent()
        assertEquals(AuthMode.UPDATE_PASSWORD, vm.state.value.mode)
        assertTrue(vm.state.value.showForm)
        vm.submit("", "new-password")
        runCurrent()
        assertEquals(1, repository.passwordUpdates)
        assertFalse(vm.state.value.showForm)
    }

    @Test fun callbackFailureIsShownWithoutLeakingServerDetails() = runTest {
        val vm = viewModel()
        runCurrent()
        repository.callbackError.value = "กรุณาขอลิงก์ใหม่"
        runCurrent()
        assertTrue(vm.state.value.showForm)
        assertEquals("กรุณาขอลิงก์ใหม่", vm.state.value.error)
        assertNull(repository.callbackError.value)
    }

    @Test fun absentConfigurationDoesNotStartNetworkWork() = runTest {
        repository.isConfigured = false
        val vm = viewModel()
        runCurrent()
        vm.google()
        vm.submit("a@b.com", "password")
        runCurrent()
        assertEquals(0, repository.calls)
        assertFalse(vm.state.value.isConfigured)
    }

    @Test fun cancellationDoesNotBecomeError() = runTest {
        val vm = viewModel()
        runCurrent()
        repository.failure = CancellationException("cancel")
        vm.submit("a@b.com", "password")
        runCurrent()
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isBusy)
    }
}
