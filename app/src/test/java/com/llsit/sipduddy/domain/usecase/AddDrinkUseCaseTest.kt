package com.llsit.sipduddy.domain.usecase

import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.fake.FakeDrinkRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AddDrinkUseCaseTest {
    private val repository = FakeDrinkRepository()
    private val clock = Clock.fixed(Instant.ofEpochMilli(12345), ZoneOffset.UTC)
    private val useCase = AddDrinkUseCase(repository, clock)

    @Test
    fun usesInjectedClockAndAcceptsAmountBoundaries() = runBlocking {
        useCase(DrinkType.TEA, 1)
        useCase(DrinkType.COFFEE, 5000)
        assertEquals(listOf(1, 5000), repository.entries.value.map { it.amountMl })
        assertTrue(repository.entries.value.all { it.timestamp == 12345L })
    }

    @Test
    fun rejectsInvalidAmountsBeforeWriting() = runBlocking {
        for (amount in listOf(-1, 0, 5001)) {
            try {
                useCase(DrinkType.WATER, amount)
                fail("accepted invalid amount")
            } catch (_: IllegalArgumentException) {
            }
        }
        assertEquals(0, repository.addCalls)
    }
}
