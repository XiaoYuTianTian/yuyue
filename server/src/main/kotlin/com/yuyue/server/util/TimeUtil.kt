package com.yuyue.server.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtil {
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun parseInstant(s: String): Instant = Instant.parse(s.replace(" ", "T").let {
        if (it.endsWith("Z") || it.contains("+") || it.indexOf('-', startIndex = 10) >= 0) it
        else "${it}Z"
    })

    fun toIso(instant: Instant): String = instant.toString()

    fun dayRange(day: LocalDate, zone: ZoneId): Pair<Instant, Instant> {
        val start = day.atStartOfDay(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }
}
