package com.llsit.sipduddy.data.mapper

import com.llsit.sipduddy.domain.model.HydrationSettings
import com.llsit.sipduddy.domain.model.QuietHours
import com.llsit.sipduddy.domain.model.ReminderSettings
import com.llsit.sipduddy.domain.model.ReminderTime
import com.llsit.sipduddy.domain.model.SettingsSyncRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

class SettingsSyncRecordMapper {
    fun encodeSettings(settings: HydrationSettings): JsonObject = buildJsonObject {
        put("goal_ml", settings.goalMl)
        put("reminder_enabled", settings.reminder.enabled)
        put("reminder_times", JsonArray(settings.reminder.times.map { JsonPrimitive(it.hour * 60 + it.minute) }))
        put("quiet_enabled", settings.reminder.quietHours.enabled)
        put("quiet_start", settings.reminder.quietHours.start.let { it.hour * 60 + it.minute })
        put("quiet_end", settings.reminder.quietHours.end.let { it.hour * 60 + it.minute })
        put("stop_at_goal", settings.reminder.stopAtGoal)
    }

    fun decodeSettings(json: JsonObject): HydrationSettings {
        fun time(value: Int): ReminderTime {
            require(value in 0..1439)
            return ReminderTime(value / 60, value % 60)
        }

        val quiet = QuietHours(
            json.getValue("quiet_enabled").jsonPrimitive.boolean,
            time(json.getValue("quiet_start").jsonPrimitive.int),
            time(json.getValue("quiet_end").jsonPrimitive.int),
        )
        return HydrationSettings(
            json.getValue("goal_ml").jsonPrimitive.int,
            ReminderSettings(
                json.getValue("reminder_enabled").jsonPrimitive.boolean,
                json.getValue("reminder_times").jsonArray.map { time(it.jsonPrimitive.int) },
                quiet,
                json.getValue("stop_at_goal").jsonPrimitive.boolean,
            ),
        )
    }

    fun encode(record: SettingsSyncRecord): String = buildJsonObject {
        put("settings", encodeSettings(record.settings))
        put("version", record.serverVersion)
        put("generation", record.localVersion)
        put("operation", record.operationId)
        put("pending", record.pending)
    }.toString()

    fun decode(owner: String, value: String): SettingsSyncRecord {
        val json = Json.parseToJsonElement(value).jsonObject
        return SettingsSyncRecord(
            owner,
            decodeSettings(json.getValue("settings").jsonObject),
            json.getValue("version").jsonPrimitive.long,
            json.getValue("generation").jsonPrimitive.long,
            json.getValue("operation").jsonPrimitive.content,
            json.getValue("pending").jsonPrimitive.boolean,
        )
    }

    fun decodeRemote(json: JsonObject): SettingsSyncRecord {
        val owner = json.getValue("owner_id").jsonPrimitive.content
        java.util.UUID.fromString(owner)
        val version = json.getValue("version").jsonPrimitive.long.also { require(it > 0) }
        val operation = json.getValue("last_operation_id").jsonPrimitive.content
        java.util.UUID.fromString(operation)
        return SettingsSyncRecord(
            owner,
            decodeSettings(json.getValue("settings").jsonObject),
            version,
            operationId = operation,
        )
    }
}
