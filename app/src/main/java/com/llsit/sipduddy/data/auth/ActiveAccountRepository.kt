package com.llsit.sipduddy.data.auth
import com.llsit.sipduddy.domain.model.AuthSession
import com.llsit.sipduddy.domain.repository.AccountRepository
import com.llsit.sipduddy.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
class ActiveAccountRepository(auth: AuthRepository, scope: CoroutineScope) : AccountRepository {
    override val owner = auth.session.map { session -> when(session) {
        AuthSession.Loading -> null
        AuthSession.SignedOut -> ""
        is AuthSession.SignedIn -> session.user.id
    } }.stateIn(scope, SharingStarted.Eagerly, null)
}
