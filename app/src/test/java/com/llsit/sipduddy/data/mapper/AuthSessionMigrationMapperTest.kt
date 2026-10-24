package com.llsit.sipduddy.data.mapper

import androidx.datastore.preferences.core.*
import com.llsit.sipduddy.data.local.preferences.AuthSessionPreferenceKeys as Keys
import org.junit.Assert.*
import org.junit.Test

class AuthSessionMigrationMapperTest {
    private val mapper = AuthSessionMigrationMapper()

    @Test fun completeLegacyPairMigratesTogetherAndMarksCompletion() {
        val result = mapper.migrate(emptyPreferences(), "old-iv", "old-ciphertext")
        assertEquals("old-iv", result[Keys.iv])
        assertEquals("old-ciphertext", result[Keys.session])
        assertEquals(true, result[Keys.migrated])
    }

    @Test fun incompleteLegacyDataNeverCreatesPartialPayload() {
        for ((iv, ciphertext) in listOf("iv" to null, null to "ciphertext", null to null)) {
            val result = mapper.migrate(emptyPreferences(), iv, ciphertext)
            assertNull(result[Keys.iv])
            assertNull(result[Keys.session])
        }
    }

    @Test fun existingDataStorePairWinsAndUnrelatedKeysRemain() {
        val other = stringPreferencesKey("other")
        val current = preferencesOf(Keys.iv to "new-iv", Keys.session to "new-ciphertext", other to "keep")
        val result = mapper.migrate(current, "old-iv", "old-ciphertext")
        assertEquals("new-iv", result[Keys.iv])
        assertEquals("new-ciphertext", result[Keys.session])
        assertEquals("keep", result[other])
    }

    @Test fun logoutMarkerPreventsLegacyResurrection() {
        val current = preferencesOf(Keys.migrated to true)
        val result = mapper.migrate(current, "old-iv", "old-ciphertext")
        assertEquals(current, result)
    }

    @Test fun partialDataStorePairIsNeverCombinedWithLegacyFields() {
        val current = preferencesOf(Keys.iv to "new-iv")
        val result = mapper.migrate(current, "old-iv", "old-ciphertext")
        assertEquals("new-iv", result[Keys.iv])
        assertNull(result[Keys.session])
    }

    @Test fun migrationIsIdempotentAndDoesNotMutateInput() {
        val initial = emptyPreferences()
        val first = mapper.migrate(initial, "old-iv", "old-ciphertext")
        assertEquals(first, mapper.migrate(first, "different-iv", "different-ciphertext"))
        assertTrue(initial.asMap().isEmpty())
    }
}
