package com.llsit.sipduddy.domain.model

data class DrinkEntry(
    val id: Long = 0,
    val drink: DrinkType,
    val amountMl: Int,
    val timestamp: Long,
    val ownerId: String? = null,
)
