package com.llsit.sipduddy.data.local.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object AuthSessionPreferenceKeys {
    // These names intentionally match the old encrypted SharedPreferences payload.
    val iv = stringPreferencesKey("iv")
    val session = stringPreferencesKey("session")
    val migrated = booleanPreferencesKey("legacy_session_migrated")
    val legacyKeys = setOf(iv.name, session.name)
}
