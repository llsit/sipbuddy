package com.llsit.sipduddy.domain.repository
interface SyncRequestScheduler {
    fun request(owner: String, force: Boolean = false)
    fun activate(owner: String)
}
