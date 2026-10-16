package com.llsit.sipduddy.presentation.mapper

import com.llsit.sipduddy.domain.model.DrinkType

val DrinkType.title: String get() = when (this) {
    DrinkType.WATER -> "น้ำเปล่า"
    DrinkType.TEA -> "ชา"
    DrinkType.COFFEE -> "กาแฟ"
    DrinkType.MILK -> "นม"
    DrinkType.JUICE -> "น้ำผลไม้"
    DrinkType.OTHER -> "เครื่องดื่มอื่น"
}

val DrinkType.symbol: String get() = when (this) {
    DrinkType.WATER -> "◉"
    DrinkType.TEA -> "❋"
    DrinkType.COFFEE -> "☕"
    DrinkType.MILK -> "♡"
    DrinkType.JUICE -> "✿"
    DrinkType.OTHER -> "◇"
}
