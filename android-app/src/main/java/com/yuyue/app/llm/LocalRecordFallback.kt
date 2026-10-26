package com.yuyue.app.llm

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object LocalRecordFallback {
    private val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun fromRaw(
        text: String,
        capturedAtMillis: Long,
        zone: ZoneId = ZoneId.of("Asia/Shanghai"),
    ): List<com.yuyue.app.data.capture.MaterializedEntry> {
        val zNow = Instant.ofEpochMilli(capturedAtMillis).atZone(zone)
        var date = zNow.toLocalDate()
        var time = zNow.toLocalTime()
        val t = text
        when {
            t.contains("后天") -> date = date.plusDays(2)
            t.contains("明天") || t.contains("明日") -> date = date.plusDays(1)
            t.contains("昨天") || t.contains("昨日") -> date = date.minusDays(1)
        }
        when {
            t.contains("早上") || t.contains("上午") -> time = LocalTime.of(9, 0)
            t.contains("中午") -> time = LocalTime.of(12, 0)
            t.contains("下午") -> time = LocalTime.of(15, 0)
            t.contains("晚上") || t.contains("傍晚") -> time = LocalTime.of(20, 0)
        }
        val start = date.atTime(time).atZone(zone).format(iso)
        val title = text.trim().replace('\n', ' ').take(20).ifBlank { "日记" }
        return listOf(
            com.yuyue.app.data.capture.MaterializedEntry(
                title = title,
                body = text.trim(),
                startAt = start,
                rawTranscript = text,
                place = "",
                people = emptyList(),
                diaryType = "life",
            ),
        )
    }
}
