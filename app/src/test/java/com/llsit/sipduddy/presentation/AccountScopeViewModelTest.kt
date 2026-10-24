package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.*
import org.junit.Test

class AccountScopeViewModelTest {
    private class TrackedViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    @Test fun sameAccountRetainsStoresButSwitchClearsPrivateState() {
        val parent = ViewModelStore()
        val scope = AccountScopeViewModel()
        parent.put("scope", scope)
        val first = scope.storeFor("A")
        val tracked = TrackedViewModel()
        first.put("child", tracked)
        assertSame(first, scope.storeFor("A"))
        assertFalse(tracked.cleared)
        val second = scope.storeFor("B")
        assertNotSame(first, second)
        assertTrue(tracked.cleared)
        assertNotSame(first, scope.storeFor("A"))
        parent.clear()
    }

    @Test fun parentClearAlsoDisposesCurrentAccountStores() {
        val parent = ViewModelStore()
        val scope = AccountScopeViewModel()
        parent.put("scope", scope)
        val tracked = TrackedViewModel()
        scope.storeFor("A").put("child", tracked)
        parent.clear()
        assertTrue(tracked.cleared)
    }
}
