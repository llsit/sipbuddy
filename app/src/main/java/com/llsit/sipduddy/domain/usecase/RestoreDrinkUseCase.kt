package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.repository.DrinkRepository

class RestoreDrinkUseCase(private val repository: DrinkRepository) {
    suspend operator fun invoke(entry: DrinkEntry): Boolean {
        require(entry.id > 0)
        return repository.restore(entry)
    }
}
