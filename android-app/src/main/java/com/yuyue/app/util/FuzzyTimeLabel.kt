package com.yuyue.app.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Display labels for diary axes: fuzzy period precision (“约晚上”), never fake HH:mm.
 *
 * Storage may still hold concrete millis (e.g. 20:00 for “晚上”); UI must not present
 * those sentinel minutes as clock precision.
 */
object FuzzyTimeLabel {
    /** Period buckets used for axis / capsule labels. */
    enum class Band(val label: String, val approxLabel: String) {
        Dawn("清晨", "约清晨"),
        Morning("上午", "约上午"),
        Noon("中午", "约中午"),
        Afternoon("下午", "约下午"),
        Evening("晚上", "约晚上"),
        Night("夜里", "约夜里"),
    }

    fun bandForHour(hour: Int): Band = when (hour) {
        in 5..8 -> Band.Dawn
        in 9..11 -> Band.Morning
        12, 13 -> Band.Noon
        in 14..17 -> Band.Afternoon
        in 18..22 -> Band.Evening
        else -> Band.Night
    }

    /**
     * Prefer spoken fuzzy marks in [rawTranscript]; otherwise derive from [startAtMillis] hour.
     * Always returns an “约…” style label for axis display (no HH:mm).
     */
    fun forEntry(
        startAtMillis: Long,
        rawTranscript: String?,
        zone: ZoneId = ZoneId.of("Asia/Shanghai"),
    ): String {
        val fromSpeech = fromSpeech(rawTranscript)
        if (fromSpeech != null) return fromSpeech.approxLabel
        val hour = Instant.ofEpochMilli(startAtMillis).atZone(zone).hour
        return bandForHour(hour).approxLabel
    }

    /** Same as [forEntry] but without the leading 约 when speech named the band explicitly. */
    fun forDetail(
        startAtMillis: Long,
        rawTranscript: String?,
        zone: ZoneId = ZoneId.of("Asia/Shanghai"),
    ): String {
        val fromSpeech = fromSpeech(rawTranscript)
        if (fromSpeech != null) return fromSpeech.approxLabel
        return forEntry(startAtMillis, null, zone)
    }

    fun fromSpeech(raw: String?): Band? {
        if (raw.isNullOrBlank()) return null
        val t = raw
        return when {
            t.contains("夜里") || t.contains("凌晨") -> Band.Night
            t.contains("傍晚") || t.contains("晚上") || t.contains("今晚") -> Band.Evening
            t.contains("下午") -> Band.Afternoon
            t.contains("中午") -> Band.Noon
            t.contains("早上") || t.contains("上午") || t.contains("清晨") -> Band.Morning
            else -> null
        }
    }

    /** True when clock minutes are likely a period sentinel (xx:00) rather than explicit time. */
    fun looksLikePeriodSentinel(startAtMillis: Long, zone: ZoneId): Boolean {
        val t = Instant.ofEpochMilli(startAtMillis).atZone(zone).toLocalTime()
        return t.minute == 0 && t.second == 0 && isCommonSentinelHour(t)
    }

    private fun isCommonSentinelHour(t: LocalTime): Boolean =
        t.hour in setOf(0, 9, 12, 15, 18, 20)
}
