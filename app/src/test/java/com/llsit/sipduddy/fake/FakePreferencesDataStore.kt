package com.llsit.sipduddy.fake

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakePreferencesDataStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    var failure: Exception? = null
    private val mutex = Mutex()
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        failure?.let { throw it }
        transform(data.value).also { data.value = it }
    }
}
