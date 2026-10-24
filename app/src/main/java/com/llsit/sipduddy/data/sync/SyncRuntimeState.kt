package com.llsit.sipduddy.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class SyncRuntimeState {
    val running = MutableStateFlow<Map<String, Int>>(emptyMap())
    fun start(owner: String) {
        running.update { it + (owner to ((it[owner] ?: 0) + 1)) }
    }

    fun finish(owner: String) {
        running.update {
            val remaining = (it[owner] ?: 1) - 1
            if (remaining <= 0) it - owner else it + (owner to remaining)
        }
    }
}
