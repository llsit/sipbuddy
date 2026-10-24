package com.llsit.sipduddy.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import com.llsit.sipduddy.data.mapper.AuthSessionMigrationMapper

internal object AuthSessionDataStoreMigration {
    fun create(context: Context, legacyName: String = "auth_session"): DataMigration<Preferences> = SharedPreferencesMigration<Preferences>(
        context = context,
        sharedPreferencesName = legacyName,
        keysToMigrate = AuthSessionPreferenceKeys.legacyKeys,
        // Also clean up stale legacy keys when both DataStore fields already exist.
        shouldRunMigration = { true },
        migrate = { legacy, current ->
            AuthSessionMigrationMapper().migrate(current, legacy.getString("iv", null), legacy.getString("session", null))
        },
    )
}
