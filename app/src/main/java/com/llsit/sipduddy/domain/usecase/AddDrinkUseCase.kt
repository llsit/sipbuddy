package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.domain.repository.DrinkRepository
import java.time.Clock

class AddDrinkUseCase(private val repository: DrinkRepository, private val clock: Clock) {
    suspend operator fun invoke(drink: DrinkType, amountMl: Int, ownerId: String? = null) {
        require(amountMl in MIN_AMOUNT_ML..MAX_AMOUNT_ML) { "ระบุปริมาณ 1–5,000 มล." }
        repository.add(DrinkEntry(drink = drink, amountMl = amountMl, timestamp = clock.millis(), ownerId = ownerId))
    }

    companion object {
        const val MIN_AMOUNT_ML = 1
        const val MAX_AMOUNT_ML = 5000
    }
}
