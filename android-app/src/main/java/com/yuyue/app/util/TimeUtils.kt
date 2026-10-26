package com.yuyue.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object TimeUtils {
    private val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val shanghai = ZoneId.of("Asia/Shanghai")

    fun nowIso(zone: ZoneId): String = Instant.now().atZone(zone).format(iso)

    fun parseIso(s: String): Instant {
        val t = s.trim().replace(" ", "T")
        runCatching { OffsetDateTime.parse(t).toInstant() }.getOrNull()?.let { return it }
        runCatching { Instant.parse(t) }.getOrNull()?.let { return it }
        runCatching { ZonedDateTime.parse(t).toInstant() }.getOrNull()?.let { return it }
        runCatching { LocalDateTime.parse(t).atZone(shanghai).toInstant() }.getOrNull()?.let { return it }
        if (t.length <= 10 && t.count { it == '-' } == 2) {
            return LocalDate.parse(t).atStartOfDay(shanghai).toInstant()
        }
        throw DateTimeParseException("无法解析时间: $s", s, 0)
    }

    fun dayRangeMillis(day: LocalDate, zone: ZoneId): Pair<Long, Long> {
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun millisToLocalDate(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun formatTime(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
}
