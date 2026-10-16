package com.llsit.sipduddy.presentation.model

import com.llsit.sipduddy.domain.model.HydrationOverview

data class HydrationUiState(
    val loading: Boolean = true,
    val overview: HydrationOverview? = null,
    val isSaving: Boolean = false,
    val isAddSheetVisible: Boolean = false,
    val message: UiMessage? = null,
    val loadError: String? = null,
)
