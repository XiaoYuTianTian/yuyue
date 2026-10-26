package com.yuyue.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DefaultStartAtTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun keepsWhenTimeExplicit() {
        val iso = "2026-08-25T15:30:00+08:00"
        val now = LocalDate.of(2026, 8, 20).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(iso, DefaultStartAt.normalize(iso, timeExplicit = true, nowMillis = now, zone = zone))
    }

    @Test
    fun midnightBecomesNowClockOnSameDay() {
        val day = LocalDate.of(2026, 8, 25)
        val iso = "2026-08-25T00:00:00+08:00"
        val now = day.atTime(14, 22, 5).atZone(zone).toInstant().toEpochMilli()
        val out = DefaultStartAt.normalize(iso, timeExplicit = false, nowMillis = now, zone = zone)
        val z = TimeUtils.parseIso(out).atZone(zone)
        assertEquals(day, z.toLocalDate())
        assertEquals(14, z.hour)
        assertEquals(22, z.minute)
        assertNotEquals(LocalTime.MIDNIGHT, z.toLocalTime())
    }

    @Test
    fun nonMidnightStillReplacesClockWhenNotExplicit() {
        val day = LocalDate.of(2026, 8, 26)
        val iso = "2026-08-26T09:00:00+08:00"
        val now = LocalDate.of(2026, 8, 20).atTime(16, 45).atZone(zone).toInstant().toEpochMilli()
        val out = DefaultStartAt.normalize(iso, timeExplicit = false, nowMillis = now, zone = zone)
        val z = TimeUtils.parseIso(out).atZone(zone)
        assertEquals(day, z.toLocalDate())
        assertEquals(16, z.hour)
        assertEquals(45, z.minute)
    }

    @Test
    fun unparseableFallsBackToNow() {
        val now = LocalDate.of(2026, 8, 20).atTime(11, 5).atZone(zone).toInstant().toEpochMilli()
        val out = DefaultStartAt.normalize("not-a-time", timeExplicit = false, nowMillis = now, zone = zone)
        val z = TimeUtils.parseIso(out).atZone(zone)
        assertEquals(11, z.hour)
        assertTrue(out.contains("+08:00") || out.contains("08:00"))
    }
}
