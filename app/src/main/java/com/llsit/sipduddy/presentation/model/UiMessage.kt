package com.llsit.sipduddy.presentation.model

data class UiMessage(val id: Long, val text: String, val undoEntry: com.llsit.sipduddy.domain.model.DrinkEntry? = null)
