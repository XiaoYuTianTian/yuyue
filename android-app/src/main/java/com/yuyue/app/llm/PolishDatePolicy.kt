package com.yuyue.app.llm

import com.yuyue.app.util.TimeUtils
import java.time.Instant
import java.time.ZoneId

object PolishDatePolicy {
    fun adjustMillis(
        lockedStartMillis: Long,
        proposedIso: String?,
        timeExplicit: Boolean,
        zone: ZoneId,
    ): Long? {
        if (!timeExplicit || proposedIso.isNullOrBlank()) return null
        val proposed = runCatching { TimeUtils.parseIso(proposedIso).toEpochMilli() }.getOrNull()
            ?: return null
        val lockedDay = Instant.ofEpochMilli(lockedStartMillis).atZone(zone).toLocalDate()
        val proposedDay = Instant.ofEpochMilli(proposed).atZone(zone).toLocalDate()
        if (lockedDay != proposedDay) return null
        return proposed
    }
}
