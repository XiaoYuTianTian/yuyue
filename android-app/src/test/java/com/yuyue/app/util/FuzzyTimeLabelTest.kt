package com.yuyue.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FuzzyTimeLabelTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun speechEveningBecomesApproxEvening() {
        val millis = LocalDate.of(2026, 9, 20).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals("约晚上", FuzzyTimeLabel.forEntry(millis, "晚上和阿哲吃了面", zone))
    }

    @Test
    fun hourBucketWithoutSpeech() {
        val millis = LocalDate.of(2026, 9, 20).atTime(15, 42).atZone(zone).toInstant().toEpochMilli()
        assertEquals("约下午", FuzzyTimeLabel.forEntry(millis, null, zone))
    }

    @Test
    fun neverEmitsClockMinutes() {
        val millis = LocalDate.of(2026, 9, 20).atTime(20, 17).atZone(zone).toInstant().toEpochMilli()
        val label = FuzzyTimeLabel.forEntry(millis, null, zone)
        assertFalse(label.contains(":"))
        assertTrue(label.startsWith("约"))
    }

    @Test
    fun bandForHourCoversNightAndDawn() {
        assertEquals(FuzzyTimeLabel.Band.Night, FuzzyTimeLabel.bandForHour(2))
        assertEquals(FuzzyTimeLabel.Band.Dawn, FuzzyTimeLabel.bandForHour(6))
        assertEquals(FuzzyTimeLabel.Band.Evening, FuzzyTimeLabel.bandForHour(20))
    }

    @Test
    fun sentinelDetection() {
        val sentinel = LocalDate.of(2026, 9, 20).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
        val precise = LocalDate.of(2026, 9, 20).atTime(20, 17).atZone(zone).toInstant().toEpochMilli()
        assertTrue(FuzzyTimeLabel.looksLikePeriodSentinel(sentinel, zone))
        assertFalse(FuzzyTimeLabel.looksLikePeriodSentinel(precise, zone))
    }
}
