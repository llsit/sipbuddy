package com.llsit.sipduddy.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// One process-wide DataStore per application context/file. Never create another instance for this file.
val Context.authSessionDataStore by preferencesDataStore(
    name = "auth_session",
    produceMigrations = { context -> listOf(AuthSessionDataStoreMigration.create(context)) },
)
