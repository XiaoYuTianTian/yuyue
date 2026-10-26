package com.yuyue.app.ui.shell

import com.yuyue.app.data.local.EntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class OnboardingFlowTest {
    @Test
    fun introToSpeakToPreviewToWrite() {
        var s = OnboardingState()
        s = OnboardingFlow.reduce(s, OnboardingEvent.StartHoldFlow)
        assertEquals(OnboardingPhase.Speak, s.phase)

        s = OnboardingFlow.reduce(s, OnboardingEvent.PreviewReady("  巷口吃了面  "))
        assertEquals(OnboardingPhase.Preview, s.phase)
        assertEquals("巷口吃了面", s.previewText)
        assertTrue(s.canConfirmPreview)

        s = OnboardingFlow.reduce(s, OnboardingEvent.ConfirmWrite)
        assertEquals(OnboardingPhase.Committing, s.phase)

        s = OnboardingFlow.reduce(s, OnboardingEvent.WriteSucceeded)
        assertEquals(OnboardingPhase.Finished, s.phase)
    }

    @Test
    fun blankPreviewStaysOnSpeak() {
        var s = OnboardingFlow.reduce(OnboardingState(phase = OnboardingPhase.Speak), OnboardingEvent.PreviewReady("   "))
        assertEquals(OnboardingPhase.Speak, s.phase)
        assertEquals("没听清，请再按住说一句", s.error)
    }

    @Test
    fun redoClearsPreview() {
        var s = OnboardingState(phase = OnboardingPhase.Preview, previewText = "旧的")
        s = OnboardingFlow.reduce(s, OnboardingEvent.Redo)
        assertEquals(OnboardingPhase.Speak, s.phase)
        assertEquals("", s.previewText)
    }

    @Test
    fun micExplainerFlag() {
        var s = OnboardingState()
        assertFalse(s.micExplainerShown)
        s = OnboardingFlow.reduce(s, OnboardingEvent.MicExplainerAcknowledged)
        assertTrue(s.micExplainerShown)
    }
}

class HomeFeedSplitTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 9, 20)

    private fun entry(id: String, day: LocalDate, hour: Int = 12) = EntryEntity(
        id = id,
        notebookId = "nb",
        entryKind = "voice",
        status = "active",
        subjectKey = null,
        polarity = null,
        title = id,
        body = id,
        rawTranscript = null,
        startAtMillis = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        endAtMillis = null,
        statedAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    @Test
    fun splitsTodayAndRecentWithoutUndoneNarrative() {
        val feed = listOf(
            entry("t1", today, 20),
            entry("t2", today, 15),
            entry("r1", today.minusDays(1)),
            entry("r2", today.minusDays(3)),
            entry("r3", today.minusDays(5)),
            entry("r4", today.minusDays(7)),
            entry("r5", today.minusDays(8)),
            entry("r6", today.minusDays(9)),
        )
        val sections = HomeFeedSplit.split(feed, today, zone, recentLimit = 5)
        assertEquals(2, sections.today.size)
        assertEquals(listOf("t1", "t2"), sections.today.map { it.id })
        assertEquals(5, sections.recent.size)
        assertFalse(sections.recent.any { it.id.startsWith("t") })
        assertEquals("今天 · 2 条已落轴", HomeFeedSplit.subtitle(sections))
    }
}
