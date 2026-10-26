package com.yuyue.app.llm

import com.yuyue.app.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LocalRecordFallbackTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val captured = LocalDate.of(2026, 8, 22).atTime(10, 15).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun tomorrowAfternoonUsesNextDayAndAfternoon() {
        val rows = LocalRecordFallback.fromRaw("明天下午去跑步", captured, zone)
        val z = TimeUtils.parseIso(rows.single().startAt).atZone(zone)
        assertEquals(LocalDate.of(2026, 8, 23), z.toLocalDate())
        assertEquals(15, z.hour)
        assertEquals("明天下午去跑步", rows.single().body)
    }

    @Test
    fun plainTextKeepsCaptureClock() {
        val rows = LocalRecordFallback.fromRaw("喝了咖啡", captured, zone)
        val z = TimeUtils.parseIso(rows.single().startAt).atZone(zone)
        assertEquals(LocalDate.of(2026, 8, 22), z.toLocalDate())
        assertEquals(10, z.hour)
        assertEquals(15, z.minute)
    }
}
