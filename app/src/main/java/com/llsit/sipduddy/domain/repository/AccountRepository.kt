package com.llsit.sipduddy.domain.repository
import kotlinx.coroutines.flow.StateFlow
interface AccountRepository {
    /** null = restoring session, empty = guest, otherwise authenticated user's ID. */
    val owner: StateFlow<String?>
}
