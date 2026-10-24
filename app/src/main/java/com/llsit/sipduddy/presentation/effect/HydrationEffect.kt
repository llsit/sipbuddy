package com.llsit.sipduddy.presentation.effect

import com.llsit.sipduddy.presentation.model.UiMessage

sealed interface HydrationEffect {
    data class ShowMessage(val message: UiMessage) : HydrationEffect
    data object DismissMessage : HydrationEffect
}
