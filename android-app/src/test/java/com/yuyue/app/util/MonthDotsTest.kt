package com.yuyue.app.util

import com.yuyue.app.data.local.EntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthDotsTest {
    @Test
    fun lifeDiaryUndone_isRed() {
        val now = 1_000_000L
        val e = EntryEntity(
            id = "1",
            notebookId = "n",
            entryKind = "note",
            status = "active",
            subjectKey = null,
            polarity = null,
            title = "晚餐",
            body = "",
            rawTranscript = null,
            startAtMillis = now - 60_000,
            endAtMillis = null,
            statedAtMillis = now,
            updatedAtMillis = now,
            done = false,
            diaryType = "life",
        )
        assertEquals(DayDot.Red, MonthDots.forEntries(listOf(e), now))
    }

    @Test
    fun allDone_isGreen() {
        val now = 1_000_000L
        val e = EntryEntity(
            id = "1",
            notebookId = "n",
            entryKind = "note",
            status = "active",
            subjectKey = null,
            polarity = null,
            title = "股票",
            body = "",
            rawTranscript = null,
            startAtMillis = now - 60_000,
            endAtMillis = null,
            statedAtMillis = now,
            updatedAtMillis = now,
            done = true,
            diaryType = "life",
        )
        assertEquals(DayDot.Green, MonthDots.forEntries(listOf(e), now))
    }
}
