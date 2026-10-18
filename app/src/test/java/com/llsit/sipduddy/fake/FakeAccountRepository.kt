package com.llsit.sipduddy.fake
import com.llsit.sipduddy.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
class FakeAccountRepository(ownerId: String? = "") : AccountRepository {
    override val owner = MutableStateFlow(ownerId)
}
