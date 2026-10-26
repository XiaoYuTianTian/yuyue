package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class PolishDatePolicyTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun sameDayExplicitAllowsClockTweak() {
        val locked = java.time.OffsetDateTime.parse("2026-04-25T10:00:00+08:00").toInstant().toEpochMilli()
        val proposed = PolishDatePolicy.adjustMillis(
            lockedStartMillis = locked,
            proposedIso = "2026-04-25T16:30:00+08:00",
            timeExplicit = true,
            zone = zone,
        )
        assertEquals(
            java.time.OffsetDateTime.parse("2026-04-25T16:30:00+08:00").toInstant().toEpochMilli(),
            proposed,
        )
    }

    @Test
    fun differentDayNeverMoves() {
        val locked = java.time.OffsetDateTime.parse("2026-04-25T10:00:00+08:00").toInstant().toEpochMilli()
        val proposed = PolishDatePolicy.adjustMillis(
            lockedStartMillis = locked,
            proposedIso = "2026-04-26T10:00:00+08:00",
            timeExplicit = true,
            zone = zone,
        )
        assertNull(proposed)
    }

    @Test
    fun implicitTimeDoesNotMoveClock() {
        val locked = java.time.OffsetDateTime.parse("2026-04-25T10:00:00+08:00").toInstant().toEpochMilli()
        val proposed = PolishDatePolicy.adjustMillis(
            lockedStartMillis = locked,
            proposedIso = "2026-04-25T16:30:00+08:00",
            timeExplicit = false,
            zone = zone,
        )
        assertNull(proposed)
    }
}
