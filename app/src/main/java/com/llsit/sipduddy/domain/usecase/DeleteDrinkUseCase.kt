package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.repository.DrinkRepository

class DeleteDrinkUseCase(private val repository: DrinkRepository) {
    suspend operator fun invoke(id: Long): DrinkEntry? {
        require(id > 0)
        return repository.deleteAndGet(id)
    }
}
