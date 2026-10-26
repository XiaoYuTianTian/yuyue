package com.yuyue.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class TimeUtilsTest {
    @Test
    fun parseIsoAcceptsOffsetWithMillis() {
        val instant = TimeUtils.parseIso("2026-08-19T22:01:57.774+08:00")
        val zoned = instant.atOffset(ZoneOffset.ofHours(8))
        assertEquals(2026, zoned.year)
        assertEquals(8, zoned.monthValue)
        assertEquals(19, zoned.dayOfMonth)
        assertEquals(22, zoned.hour)
        assertEquals(1, zoned.minute)
        assertEquals(57, zoned.second)
    }

    @Test
    fun parseIsoAcceptsUtcZ() {
        val instant = TimeUtils.parseIso("2026-08-19T14:01:57Z")
        assertEquals(14, instant.atOffset(ZoneOffset.UTC).hour)
    }
}
