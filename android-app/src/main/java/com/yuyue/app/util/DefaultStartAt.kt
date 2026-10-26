package com.yuyue.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 用户未明确说出时刻时：保留模型日期，时刻改用「现在」，避免落成 00:00。
 */
object DefaultStartAt {
    private val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun normalize(
        isoStart: String,
        timeExplicit: Boolean,
        nowMillis: Long,
        zone: ZoneId,
    ): String {
        if (timeExplicit) {
            return runCatching {
                TimeUtils.parseIso(isoStart).atZone(zone).format(iso)
            }.getOrDefault(isoStart)
        }
        val nowZ = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val parsed = runCatching { TimeUtils.parseIso(isoStart).atZone(zone) }.getOrNull()
            ?: return nowZ.format(iso)
        return parsed.toLocalDate().atTime(nowZ.toLocalTime()).atZone(zone).format(iso)
    }
}
