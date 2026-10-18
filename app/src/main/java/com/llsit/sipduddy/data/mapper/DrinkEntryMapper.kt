package com.llsit.sipduddy.data.mapper

import com.llsit.sipduddy.data.local.entity.DrinkEntity
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType

class DrinkEntryMapper {
    fun toDomain(entity: DrinkEntity): DrinkEntry = DrinkEntry(
        id = entity.id,
        drink = DrinkType.entries.firstOrNull { it.name == entity.drink } ?: DrinkType.OTHER,
        amountMl = entity.amountMl,
        timestamp = entity.timestamp,
        ownerId = entity.ownerId,
    )

    fun toEntity(entry: DrinkEntry): DrinkEntity = DrinkEntity(
        id = entry.id,
        drink = entry.drink.name,
        amountMl = entry.amountMl,
        timestamp = entry.timestamp,
        ownerId = entry.ownerId ?: "",
    )
}
