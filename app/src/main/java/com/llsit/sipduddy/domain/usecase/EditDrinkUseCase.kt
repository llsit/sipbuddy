package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.repository.DrinkRepository

class EditDrinkUseCase(private val repository: DrinkRepository) {
    suspend operator fun invoke(id: Long, drink: DrinkType, amountMl: Int) {
        require(id > 0)
        require(amountMl in AddDrinkUseCase.MIN_AMOUNT_ML..AddDrinkUseCase.MAX_AMOUNT_ML) { "ระบุปริมาณ 1–5,000 มล." }
        val entry = repository.getById(id)
        require(entry != null) { "ไม่พบรายการนี้แล้ว" }
        require(repository.update(entry.copy(drink = drink, amountMl = amountMl))) { "ไม่พบรายการนี้แล้ว" }
    }
}
