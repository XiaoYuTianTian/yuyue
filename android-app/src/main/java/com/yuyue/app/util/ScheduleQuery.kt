package com.yuyue.app.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class DateRange(val start: LocalDate, val endExclusive: LocalDate)

/**
 * 从中文/数字问句里解析日程范围，供「去年某天做了什么」类回忆走本地日历检索。
 */
object ScheduleQuery {
    private val ymd = Regex("""(\d{4})[-/年.](\d{1,2})[-/月.](\d{1,2})日?""")
    private val md = Regex("""(\d{1,2})月(\d{1,2})日""")
    private val yearMonth = Regex("""(\d{4})年(\d{1,2})月""")
    private val lastYearMonth = Regex("""去年(\d{1,2})月(?:(\d{1,2})日)?""")
    private val thisMonthDay = Regex("""本月(\d{1,2})日""")

    fun parse(query: String, now: LocalDate = LocalDate.now(ZoneId.of("Asia/Shanghai"))): DateRange? {
        val q = query.trim()
        ymd.find(q)?.let { m ->
            val d = dateOrNull(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
            return d?.let { DateRange(it, it.plusDays(1)) }
        }
        lastYearMonth.find(q)?.let { m ->
            val year = now.year - 1
            val month = m.groupValues[1].toInt()
            val day = m.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull()
            return if (day != null) {
                dateOrNull(year, month, day)?.let { DateRange(it, it.plusDays(1)) }
            } else {
                monthRange(year, month)
            }
        }
        if (q.contains("去年")) {
            md.find(q)?.let { m ->
                val d = dateOrNull(now.year - 1, m.groupValues[1].toInt(), m.groupValues[2].toInt())
                return d?.let { DateRange(it, it.plusDays(1)) }
            }
            yearMonth.find(q)?.let { m ->
                return monthRange(m.groupValues[1].toInt(), m.groupValues[2].toInt())
            }
            if (!q.contains("月")) {
                return DateRange(LocalDate.of(now.year - 1, 1, 1), LocalDate.of(now.year, 1, 1))
            }
        }
        yearMonth.find(q)?.let { m ->
            return monthRange(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        thisMonthDay.find(q)?.let { m ->
            val d = dateOrNull(now.year, now.monthValue, m.groupValues[1].toInt())
            return d?.let { DateRange(it, it.plusDays(1)) }
        }
        md.find(q)?.let { m ->
            val d = dateOrNull(now.year, m.groupValues[1].toInt(), m.groupValues[2].toInt())
            return d?.let { DateRange(it, it.plusDays(1)) }
        }
        when {
            q.contains("今天") || q.contains("今日") -> return DateRange(now, now.plusDays(1))
            q.contains("昨天") -> return DateRange(now.minusDays(1), now)
            q.contains("前天") -> return DateRange(now.minusDays(2), now.minusDays(1))
            q.contains("本周") || q.contains("这周") -> {
                val start = now.minusDays((now.dayOfWeek.value - 1).toLong())
                return DateRange(start, start.plusDays(7))
            }
            q.contains("上周") -> {
                val thisMonday = now.minusDays((now.dayOfWeek.value - 1).toLong())
                return DateRange(thisMonday.minusDays(7), thisMonday)
            }
        }
        return null
    }

    private fun monthRange(year: Int, month: Int): DateRange? {
        if (month !in 1..12) return null
        val ym = YearMonth.of(year, month)
        return DateRange(ym.atDay(1), ym.plusMonths(1).atDay(1))
    }

    private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? = runCatching {
        LocalDate.of(year, month, day)
    }.getOrNull()
}
