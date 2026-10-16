package com.llsit.sipduddy.fake

import com.llsit.sipduddy.domain.model.DateContext
import com.llsit.sipduddy.domain.time.DateProvider
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDateProvider(initial: DateContext) : DateProvider {
    override val dates = MutableStateFlow(initial)
}
