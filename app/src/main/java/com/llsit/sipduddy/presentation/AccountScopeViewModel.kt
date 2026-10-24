package com.llsit.sipduddy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

// Lifecycle infrastructure: retain the active MVI stores on rotation, clear them on account switch.
internal class AccountScopeViewModel : ViewModel() {
    private var initialized = false
    private var owner: String? = null
    private var store = ViewModelStore()

    fun storeFor(ownerId: String?): ViewModelStore {
        if (!initialized || owner != ownerId) {
            store.clear()
            store = ViewModelStore()
            owner = ownerId
            initialized = true
        }
        return store
    }

    override fun onCleared() {
        store.clear()
    }
}
