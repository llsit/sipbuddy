package com.llsit.sipduddy.presentation.mutation

import com.llsit.sipduddy.domain.model.*
import com.llsit.sipduddy.presentation.model.*

internal sealed interface HydrationMutation {
    data object RetryStarted : HydrationMutation
    data class Loaded(val overview: HydrationOverview) : HydrationMutation
    data object LoadFailed : HydrationMutation
    data class Saving(val value: Boolean) : HydrationMutation
    data class EditorOpened(val entry: DrinkEntry?) : HydrationMutation
    data object EditorClosed : HydrationMutation
    data class MessageShown(val message: UiMessage) : HydrationMutation
    data class MessageHandled(val id: Long) : HydrationMutation
    data class Navigated(val destination: AppDestination) : HydrationMutation
    data class DeleteRequested(val id: Long?) : HydrationMutation
}
