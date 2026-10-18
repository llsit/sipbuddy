package com.llsit.sipduddy.data.time

import com.llsit.sipduddy.domain.model.DateContext
import com.llsit.sipduddy.domain.time.DateProvider
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

class SystemDateProvider(private val clock: Clock) : DateProvider {
    override val dates = flow {
        while (currentCoroutineContext().isActive) {
            val zone = ZoneId.systemDefault()
            emit(DateContext(clock.instant().atZone(zone).toLocalDate(), zone))
            delay(15_000)
        }
    }.distinctUntilChanged()
}
