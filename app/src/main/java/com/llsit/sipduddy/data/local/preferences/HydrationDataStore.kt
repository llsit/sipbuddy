package com.llsit.sipduddy.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.hydrationDataStore by preferencesDataStore(name = "sipbuddy_settings")
