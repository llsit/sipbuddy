package com.llsit.sipduddy.presentation.model

import com.llsit.sipduddy.domain.model.SyncStatus

data class SyncUiState(val status: SyncStatus = SyncStatus(), val importing: Boolean = false, val message: String? = null)
