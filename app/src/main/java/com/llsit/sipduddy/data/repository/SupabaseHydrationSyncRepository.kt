package com.llsit.sipduddy.data.repository
import com.llsit.sipduddy.data.auth.SupabaseClientProvider
import com.llsit.sipduddy.data.mapper.RemoteDrinkMapper
import com.llsit.sipduddy.domain.model.SyncRecord
import com.llsit.sipduddy.domain.model.SyncPushResult
import com.llsit.sipduddy.domain.repository.RemoteHydrationSyncRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.*

class SupabaseHydrationSyncRepository(private val provider: SupabaseClientProvider,private val mapper: RemoteDrinkMapper) : RemoteHydrationSyncRepository {
    private suspend fun client(owner: String) = checkNotNull(provider.client).also {
        it.auth.awaitInitialization()
        check(it.auth.currentUserOrNull()?.id == owner) { "No current session for this account" }
    }
    override suspend fun push(record: SyncRecord): SyncPushResult {
        // RPC checks p_owner against auth.uid(), even if the SDK's token changes during the request.
        val data=client(record.ownerId).postgrest.rpc("apply_hydration_change",mapper.encode(record)).data
        val result=Json.parseToJsonElement(data).jsonObject
        return SyncPushResult(result.getValue("applied").jsonPrimitive.boolean,mapper.decode(result.getValue("record").jsonObject))
    }
    override suspend fun page(owner: String,afterId: String?,limit: Int): List<SyncRecord> {
        val data=client(owner).from("hydration_entries").select {
            filter { eq("owner_id",owner); if (afterId != null) gt("entry_id",afterId) }
            order("entry_id",Order.ASCENDING)
            limit(limit.toLong())
        }.data
        return Json.parseToJsonElement(data).jsonArray.map { mapper.decode(it.jsonObject) }
    }
}
