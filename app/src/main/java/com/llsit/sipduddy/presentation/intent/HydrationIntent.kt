package com.llsit.sipduddy.presentation.intent

import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.presentation.model.AppDestination

sealed interface HydrationIntent {
    data object Retry : HydrationIntent
    data object OpenAddDrink : HydrationIntent
    data class OpenEditDrink(val entry: DrinkEntry) : HydrationIntent
    data object DismissDrinkEditor : HydrationIntent
    data class SaveDrink(val drink: DrinkType, val amountMl: Int) : HydrationIntent
    data class DeleteDrink(val id: Long) : HydrationIntent
    data class UndoDelete(val messageId: Long) : HydrationIntent
    data class SaveSettings(val settings: HydrationSettings) : HydrationIntent
    data class MessageHandled(val id: Long) : HydrationIntent
    data class Navigate(val destination: AppDestination) : HydrationIntent
    data class RequestDelete(val id: Long) : HydrationIntent
    data object DismissDelete : HydrationIntent
}
