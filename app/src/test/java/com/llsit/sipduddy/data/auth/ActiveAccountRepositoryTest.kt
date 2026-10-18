package com.llsit.sipduddy.data.auth
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.domain.model.AuthUser
import com.llsit.sipduddy.fake.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveAccountRepositoryTest {
    @Test fun losingNetworkRetainsOwnerWhileExplicitLogoutReturnsToGuest() = runTest {
        val auth = FakeAuthRepository()
        auth.session.value = AuthSession.Loading
        val accounts = ActiveAccountRepository(auth, backgroundScope)
        runCurrent()
        assertNull(accounts.owner.value)
        val user = AuthUser("A", "a@example.com")
        auth.session.value = AuthSession.SignedIn(user)
        runCurrent()
        assertEquals("A", accounts.owner.value)
        auth.session.value = AuthSession.SignedIn(user, isOffline = true)
        runCurrent()
        assertEquals("A", accounts.owner.value)
        auth.session.value = AuthSession.SignedOut
        runCurrent()
        assertEquals("", accounts.owner.value)
    }
}
