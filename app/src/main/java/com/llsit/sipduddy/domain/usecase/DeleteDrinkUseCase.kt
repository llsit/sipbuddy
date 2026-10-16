package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.repository.DrinkRepository

class DeleteDrinkUseCase(private val repository: DrinkRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0) { "รายการไม่ถูกต้อง" }
        repository.delete(id)
    }
}
