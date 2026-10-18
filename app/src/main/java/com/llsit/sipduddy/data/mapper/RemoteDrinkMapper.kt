package com.llsit.sipduddy.data.mapper
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.model.SyncRecord
import java.util.UUID
import kotlinx.serialization.json.*
class RemoteDrinkMapper {
    fun decode(value: JsonObject): SyncRecord {
        fun text(key: String) = value.getValue(key).jsonPrimitive.content
        val owner = text("owner_id")
        val id = text("entry_id")
        val operation = text("last_operation_id")
        UUID.fromString(owner)
        UUID.fromString(id)
        UUID.fromString(operation)
        val amount = value.getValue("amount_ml").jsonPrimitive.int
        val version = value.getValue("version").jsonPrimitive.long
        require(amount in 1..5000 && version > 0)
        return SyncRecord(
            owner,
            id,
            text("drink"),
            amount,
            value.getValue("consumed_at").jsonPrimitive.long,
            value.getValue("deleted").jsonPrimitive.boolean,
            version,
            operation,
        )
    }
    fun encode(record: SyncRecord) = buildJsonObject {
        put("p_owner", record.ownerId)
        put("p_entry", record.remoteId)
        put("p_drink", if (DrinkType.entries.any { it.name == record.drink }) record.drink else DrinkType.OTHER.name)
        put("p_amount", record.amountMl)
        put("p_consumed", record.timestamp)
        put("p_deleted", record.deleted)
        put("p_base_version", record.serverVersion)
        put("p_operation", record.operationId)
        put("p_restore", record.allowRestore)
    }
}
