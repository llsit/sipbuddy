package com.llsit.sipduddy.presentation.model

import com.llsit.sipduddy.domain.model.HydrationOverview

internal sealed interface OverviewLoadState {
    data object Loading : OverviewLoadState
    data class Loaded(val overview: HydrationOverview) : OverviewLoadState
    data object Failed : OverviewLoadState
}
