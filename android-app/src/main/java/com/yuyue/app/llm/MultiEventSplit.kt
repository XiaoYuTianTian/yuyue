package com.yuyue.app.llm

import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object MultiEventSplit {
    private val timeMarks = listOf("明早", "明天", "上午", "下午", "晚上", "中午", "今晚")

    fun entries(userText: String, nowIso: String): List<StructuredEntry> {
        val parts = userText.split('，', ',', '、').map { it.trim() }.filter { it.length >= 2 }
        if (parts.size < 2) return emptyList()
        val marked = parts.count { part -> timeMarks.any { it in part } }
        if (marked < 2) return emptyList()
        val now = OffsetDateTime.parse(nowIso)
        var dayOffset = 0L
        return parts.map { part ->
            if ("明早" in part || "明天" in part) dayOffset = 1
            val day = now.toLocalDate().plusDays(dayOffset)
            val time = when {
                "早" in part || "上午" in part -> LocalTime.of(9, 0)
                "下午" in part -> LocalTime.of(15, 0)
                "晚" in part || "今晚" in part -> LocalTime.of(19, 0)
                "中午" in part -> LocalTime.of(12, 0)
                else -> now.toLocalTime()
            }
            val start = OffsetDateTime.of(day, time, now.offset)
            StructuredEntry(
                title = part.take(20),
                startAt = start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                body = part,
                raw = part,
            )
        }
    }
}
