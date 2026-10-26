package com.yuyue.app.notify

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class OnThisDayWorkTest {
    @Test
    fun delayIsPositiveAndUnderOneDay() {
        val zone = ZoneId.of("Asia/Shanghai")
        val now = ZonedDateTime.of(2026, 9, 20, 10, 0, 0, 0, zone)
        val delay = OnThisDayWork.millisUntilNext(LocalTime.of(9, 0), zone, now)
        assertTrue(delay > 0)
        assertTrue(delay <= 24L * 60 * 60 * 1000)
    }

    @Test
    fun beforeNineSameDay() {
        val zone = ZoneId.of("Asia/Shanghai")
        val now = ZonedDateTime.of(2026, 9, 20, 8, 0, 0, 0, zone)
        val delay = OnThisDayWork.millisUntilNext(LocalTime.of(9, 0), zone, now)
        assertTrue(delay == 60L * 60 * 1000)
    }
}
