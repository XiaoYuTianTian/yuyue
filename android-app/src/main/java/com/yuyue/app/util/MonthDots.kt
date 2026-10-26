package com.yuyue.app.util

enum class DayDot { Green, Red }

object MonthDots {
    /** 当天有任何未完成日记 → 红；否则绿。 */
    fun forEntries(entries: List<com.yuyue.app.data.local.EntryEntity>, nowMillis: Long): DayDot {
        val pending = entries.any { !it.done }
        return if (pending) DayDot.Red else DayDot.Green
    }

    fun forFlags(doneFlags: List<Boolean>): DayDot =
        if (doneFlags.any { !it }) DayDot.Red else DayDot.Green

    fun defaultDone(startAtMillis: Long, nowMillis: Long): Boolean =
        startAtMillis <= nowMillis
}
