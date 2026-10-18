package com.llsit.sipduddy.domain.usecase
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.fake.FakeDrinkRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class EditAndRestoreDrinkUseCaseTest {
    private val entry = DrinkEntry(9, DrinkType.WATER, 250, 1234567)

    @Test fun editPreservesIdentityAndOriginalDate() = runTest {
        val repo = FakeDrinkRepository().apply { entries.value = listOf(entry) }
        EditDrinkUseCase(repo)(9, DrinkType.TEA, 150)
        assertEquals(entry.copy(drink = DrinkType.TEA, amountMl = 150), repo.entries.value.single())
    }

    @Test fun invalidAmountAndMissingEntryCannotCreateRecords() = runTest {
        val repo = FakeDrinkRepository().apply { entries.value = listOf(entry) }
        for ((id, amount) in listOf(9L to 0, 9L to 5001, 99L to 250)) {
            try {
                EditDrinkUseCase(repo)(id, DrinkType.TEA, amount)
                fail("accepted invalid edit")
            } catch (_: IllegalArgumentException) { }
        }
        assertEquals(listOf(entry), repo.entries.value)
    }

    @Test fun deleteSnapshotRestoresExactRecordOnlyOnce() = runTest {
        val repo = FakeDrinkRepository().apply { entries.value = listOf(entry) }
        val deleted = DeleteDrinkUseCase(repo)(9)!!
        assertNull(DeleteDrinkUseCase(repo)(9))
        assertTrue(RestoreDrinkUseCase(repo)(deleted))
        assertFalse(RestoreDrinkUseCase(repo)(deleted))
        assertEquals(listOf(entry), repo.entries.value)
    }

    @Test fun staleRestoreNeverOverwritesExistingEdit() = runTest {
        val changed = entry.copy(amountMl = 500)
        val repo = FakeDrinkRepository().apply { entries.value = listOf(changed) }
        assertFalse(RestoreDrinkUseCase(repo)(entry))
        assertEquals(listOf(changed), repo.entries.value)
    }
}
