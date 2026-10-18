package com.llsit.sipduddy.data.repository

import com.llsit.sipduddy.data.auth.SupabaseClientProvider
import com.llsit.sipduddy.data.mapper.SettingsSyncRecordMapper
import com.llsit.sipduddy.domain.model.SettingsSyncRecord
import com.llsit.sipduddy.domain.model.SettingsSyncPushResult
import com.llsit.sipduddy.domain.repository.RemoteSettingsSyncRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.*

class SupabaseSettingsSyncRepository(
    private val provider: SupabaseClientProvider,
    private val mapper: SettingsSyncRecordMapper,
) : RemoteSettingsSyncRepository {
    private suspend fun client(owner: String) = checkNotNull(provider.client).also {
        it.auth.awaitInitialization()
        check(it.auth.currentUserOrNull()?.id == owner) { "No current session for this account" }
    }
    override suspend fun push(record: SettingsSyncRecord): SettingsSyncPushResult {
        val result = client(record.ownerId).postgrest.rpc("apply_hydration_settings_change", buildJsonObject {
            put("p_owner", record.ownerId)
            put("p_settings", mapper.encodeSettings(record.settings))
            put("p_base_version", record.serverVersion)
            put("p_operation", record.operationId)
        }).data.let { Json.parseToJsonElement(it).jsonObject }
        return SettingsSyncPushResult(result.getValue("applied").jsonPrimitive.boolean,
            mapper.decodeRemote(result.getValue("record").jsonObject))
    }
    override suspend fun read(owner: String): SettingsSyncRecord? {
        val rows = client(owner).from("hydration_settings").select { filter { eq("owner_id", owner) }; limit(1) }
            .data.let { Json.parseToJsonElement(it).jsonArray }
        return rows.firstOrNull()?.let { mapper.decodeRemote(it.jsonObject) }
    }
}
