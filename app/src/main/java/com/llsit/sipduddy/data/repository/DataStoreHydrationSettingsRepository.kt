package com.llsit.sipduddy.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.llsit.sipduddy.data.mapper.HydrationSettingsMapper
import com.llsit.sipduddy.data.mapper.SettingsSyncRecordMapper
import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.domain.repository.*
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class DataStoreHydrationSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val mapper: HydrationSettingsMapper,
    private val accounts: AccountRepository? = null,
    private val syncMapper: SettingsSyncRecordMapper = SettingsSyncRecordMapper(),
) : HydrationSettingsRepository,
    LocalSettingsSyncRepository {
    private fun key(owner: String) = stringPreferencesKey("account_settings_v1:$owner")
    private fun record(owner: String, preferences: Preferences): SettingsSyncRecord = preferences[key(owner)]?.let { syncMapper.decode(owner, it) }
        // Legacy shared settings belong to the guest workspace only.
        ?: SettingsSyncRecord(owner, if (owner.isEmpty()) mapper.toDomain(preferences) else HydrationSettings())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val settings = if (accounts == null) {
        dataStore.data.map(mapper::toDomain).distinctUntilChanged()
    } else {
        accounts.owner.filterNotNull().flatMapLatest { owner ->
            observeRecord(owner).map { it.settings.copy(ownerId = owner) }
        }.distinctUntilChanged()
    }

    override suspend fun save(settings: HydrationSettings) = saveForOwner(settings, accounts?.owner?.value)

    override suspend fun saveForOwner(settings: HydrationSettings, expectedOwner: String?) {
        if (accounts == null) {
            dataStore.edit { mapper.write(settings, it) }
            return
        }
        val owner = requireNotNull(expectedOwner) { "บัญชีกำลังโหลด กรุณาลองอีกครั้ง" }
        dataStore.edit { preferences ->
            require(accounts.owner.value == owner) { "บัญชีเปลี่ยนแล้ว กรุณาลองใหม่" }
            val previous = record(owner, preferences)
            preferences[key(owner)] = syncMapper.encode(
                previous.copy(
                    settings = settings.copy(ownerId = null),
                    localVersion = previous.localVersion + 1,
                    operationId = UUID.randomUUID().toString(),
                    pending = owner.isNotEmpty(),
                ),
            )
        }
    }
    override fun observeRecord(owner: String): Flow<SettingsSyncRecord> = dataStore.data.map { record(owner, it) }.distinctUntilChanged()
    override suspend fun read(owner: String) = observeRecord(owner).first()

    override suspend fun acknowledge(sent: SettingsSyncRecord, remote: SettingsSyncRecord) {
        require(sent.ownerId == remote.ownerId)
        dataStore.edit { preferences ->
            val current = record(sent.ownerId, preferences)
            if (remote.serverVersion < current.serverVersion) return@edit
            val next = if (current.operationId == sent.operationId && current.localVersion == sent.localVersion) {
                remote.copy(localVersion = current.localVersion, pending = false)
            } else {
                current.copy(serverVersion = remote.serverVersion)
            }
            preferences[key(sent.ownerId)] = syncMapper.encode(next)
        }
    }
    override suspend fun merge(remote: SettingsSyncRecord) {
        dataStore.edit { preferences ->
            val current = record(remote.ownerId, preferences)
            if (!current.pending && remote.serverVersion > current.serverVersion) {
                preferences[key(remote.ownerId)] = syncMapper.encode(remote.copy(localVersion = current.localVersion, pending = false))
            }
        }
    }
}
