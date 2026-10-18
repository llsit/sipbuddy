package com.llsit.sipduddy.data.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemDateProviderTest {
    @Test
    fun usesInjectedInstantAndCurrentLocalTimezone() = runBlocking {
        val instant = Instant.parse("2026-10-16T18:00:00Z")
        val context = SystemDateProvider(Clock.fixed(instant, ZoneOffset.UTC)).dates.first()
        assertEquals(ZoneId.systemDefault(), context.zone)
        assertEquals(instant.atZone(context.zone).toLocalDate(), context.date)
    }
}
