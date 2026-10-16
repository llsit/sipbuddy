package com.llsit.sipduddy.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.llsit.sipduddy.domain.*
import kotlinx.coroutines.flow.map

private val Context.settings by preferencesDataStore("sipbuddy_settings")

class LocalHydrationRepository(context: Context, private val dao: DrinkDao) : HydrationRepository {
    private val store = context.settings
    private val goal = intPreferencesKey("goal_ml")
    private val enabled = booleanPreferencesKey("reminder_enabled")
    private val hour = intPreferencesKey("reminder_hour")
    private val minute = intPreferencesKey("reminder_minute")
    override val entries = dao.observe().map { rows -> rows.map {
        DrinkEntry(it.id, Drink.valueOf(it.drink), it.amountMl, it.timestamp)
    } }
    override val preferences = store.data.map { Preferences(it[goal] ?: 2000, it[enabled] ?: false, it[hour] ?: 10, it[minute] ?: 0) }
    override suspend fun add(entry: DrinkEntry) = dao.insert(DrinkEntity(drink = entry.drink.name, amountMl = entry.amountMl, timestamp = entry.timestamp))
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun savePreferences(preferences: Preferences) {
        store.edit {
            it[goal] = preferences.goalMl
            it[enabled] = preferences.reminderEnabled
            it[hour] = preferences.hour
            it[minute] = preferences.minute
        }
    }
}
