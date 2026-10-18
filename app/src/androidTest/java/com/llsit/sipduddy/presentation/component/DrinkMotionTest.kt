package com.llsit.sipduddy.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.llsit.sipduddy.domain.model.DrinkEntry
import com.llsit.sipduddy.domain.model.DrinkType
import com.llsit.sipduddy.ui.theme.SipDuddyTheme
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DrinkMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectedChipRemainsClickableDuringAndAfterMotion() {
        val selected = mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SipDuddyTheme {
                AnimatedSelectionChip(selected.value, true, "น้ำ", { selected.value = !selected.value })
            }
        }
        compose.onNodeWithText("น้ำ").assertIsNotSelected().performClick()
        compose.mainClock.advanceTimeBy(80)
        compose.onNodeWithText("น้ำ").assertIsSelected().performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("น้ำ").assertIsNotSelected()
    }

    @Test fun editFinishesWithUpdatedAmountAndSingleUsableDeleteAction() {
        val entry = mutableStateOf(DrinkEntry(1, DrinkType.WATER, 250, 100))
        var deleted: DrinkEntry? = null
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SipDuddyTheme {
                DrinkEntryRow(entry.value, ZoneOffset.UTC, false, {}, { deleted = it }, Modifier.fillMaxWidth())
            }
        }
        compose.runOnIdle { entry.value = entry.value.copy(drink = DrinkType.TEA, amountMl = 400) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("400 มล.").assertExists()
        compose.onNodeWithText("250 มล.").assertDoesNotExist()
        compose.onNodeWithText("ลบ").performClick()
        compose.runOnIdle { assertEquals(entry.value, deleted) }
    }

    @Test fun deletedItemLeavesLazyListAndUndoCanReinsertSameIdentity() {
        val original = DrinkEntry(1, DrinkType.WATER, 250, 100)
        val rows = mutableStateOf(listOf(original))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SipDuddyTheme {
                LazyColumn {
                    items(rows.value, key = { it.id }) { entry ->
                        DrinkEntryRow(
                            entry,
                            ZoneOffset.UTC,
                            false,
                            {},
                            { removed -> rows.value = rows.value.filterNot { it.id == removed.id } },
                            Modifier.animateItem(),
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("ลบ").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("250 มล.").assertDoesNotExist()
        compose.runOnIdle { rows.value = listOf(original) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("250 มล.").assertExists()
        compose.onNodeWithText("ลบ").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("250 มล.").assertDoesNotExist()
    }
}
