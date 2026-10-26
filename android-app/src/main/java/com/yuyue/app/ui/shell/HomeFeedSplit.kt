package com.yuyue.app.ui.shell

import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.util.TimeUtils
import java.time.LocalDate
import java.time.ZoneId

/**
 * Home narrative: today + a few recent — never “接下来 / 未完成” as the default story.
 */
data class HomeFeedSections(
    val today: List<EntryEntity>,
    val recent: List<EntryEntity>,
) {
    val todayCount: Int get() = today.size
}

object HomeFeedSplit {
    const val RECENT_LIMIT = 5

    fun split(
        feed: List<EntryEntity>,
        today: LocalDate,
        zone: ZoneId,
        recentLimit: Int = RECENT_LIMIT,
    ): HomeFeedSections {
        val todayEntries = feed
            .filter { TimeUtils.millisToLocalDate(it.startAtMillis, zone) == today }
            .sortedByDescending { it.startAtMillis }
        val todayIds = todayEntries.map { it.id }.toSet()
        val recent = feed
            .filter { it.id !in todayIds }
            .sortedByDescending { it.startAtMillis }
            .take(recentLimit)
        return HomeFeedSections(today = todayEntries, recent = recent)
    }

    fun subtitle(sections: HomeFeedSections): String = when {
        sections.todayCount > 0 -> "今天 · ${sections.todayCount} 条已落轴"
        sections.recent.isNotEmpty() -> "近期 · ${sections.recent.size} 条"
        else -> "按住说一句，落到今天"
    }
}
