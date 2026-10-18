package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.fake.FakeDrinkRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class DeleteDrinkUseCaseTest {
    @Test
    fun deletesOnlySelectedRecordAndIsIdempotent() = runBlocking {
        val repository = FakeDrinkRepository()
        repository.entries.value =
            listOf(DrinkEntry(1, DrinkType.WATER, 250, 1), DrinkEntry(2, DrinkType.TEA, 150, 2))
        val useCase = DeleteDrinkUseCase(repository)
        useCase(1)
        useCase(1)
        assertEquals(listOf(2L), repository.entries.value.map { it.id })
    }

    @Test
    fun rejectsUnpersistedId() = runBlocking {
        try {
            DeleteDrinkUseCase(FakeDrinkRepository())(0)
            fail("accepted invalid id")
        } catch (_: IllegalArgumentException) {
        }
    }
}
