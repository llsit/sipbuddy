package com.llsit.sipduddy.data.mapper

import androidx.datastore.preferences.core.Preferences
import com.llsit.sipduddy.data.local.preferences.AuthSessionPreferenceKeys as Keys

internal class AuthSessionMigrationMapper {
    fun migrate(current: Preferences, legacyIv: String?, legacySession: String?): Preferences {
        val result = current.toMutablePreferences()
        // Never merge individual crypto fields or restore old tokens after a DataStore logout.
        if (current[Keys.migrated] != true && current[Keys.iv] == null && current[Keys.session] == null &&
            legacyIv != null && legacySession != null
        ) {
            result[Keys.iv] = legacyIv
            result[Keys.session] = legacySession
        }
        result[Keys.migrated] = true
        return result.toPreferences()
    }
}
