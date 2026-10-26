package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SufficiencyTest {
    @Test
    fun meetingTwoMissingAsks() {
        val turn = StructuredTurn(
            intent = TurnIntent.Record,
            timeExplicit = false,
            entries = listOf(entry(diaryType = "meeting", place = "", people = emptyList())),
        )
        assertEquals(listOf("time", "place", "people"), Sufficiency.missingSlots(turn))
        assertTrue(Sufficiency.shouldClarify(turn, "要开会", 0))
    }

    @Test
    fun meetingOneMissingSaves() {
        val turn = StructuredTurn(
            intent = TurnIntent.Record,
            timeExplicit = true,
            entries = listOf(entry(diaryType = "meeting", place = "会议室", people = emptyList())),
        )
        assertEquals(listOf("people"), Sufficiency.missingSlots(turn))
        assertFalse(Sufficiency.shouldClarify(turn, "明天下午三点会议室开会", 0))
    }

    @Test
    fun lifeWithTitleDoesNotAskPlacePeople() {
        val turn = StructuredTurn(
            intent = TurnIntent.Record,
            timeExplicit = false,
            entries = listOf(
                entry(
                    title = "有点累",
                    diaryType = "life",
                    place = "",
                    people = emptyList(),
                    body = "今天有点累",
                ),
            ),
        )
        assertEquals(listOf("time"), Sufficiency.missingSlots(turn))
        assertFalse(Sufficiency.shouldClarify(turn, "今天有点累", 0))
    }

    @Test
    fun foodIgnoresPlacePeopleSlots() {
        val turn = StructuredTurn(
            intent = TurnIntent.Record,
            timeExplicit = true,
            missingSlots = listOf("place", "people"),
            entries = listOf(entry(title = "午饭", diaryType = "food", body = "吃了面")),
        )
        assertEquals(emptyList<String>(), Sufficiency.missingSlots(turn))
        assertFalse(Sufficiency.shouldClarify(turn, "中午吃了面", 0))
    }

    @Test
    fun forceSaveSkipsClarify() {
        val turn = StructuredTurn(
            intent = TurnIntent.Clarify,
            timeExplicit = false,
            entries = listOf(entry(diaryType = "meeting")),
        )
        assertTrue(Sufficiency.forceSave("先记下，以后再补"))
        assertFalse(Sufficiency.shouldClarify(turn, "先记下", 0))
    }

    @Test
    fun thirdRoundNeverAsks() {
        val turn = StructuredTurn(
            intent = TurnIntent.Record,
            missingSlots = listOf("time", "place"),
            entries = listOf(entry(diaryType = "meeting")),
        )
        assertTrue(Sufficiency.shouldClarify(turn, "还要开会", 1))
        assertFalse(Sufficiency.shouldClarify(turn, "还要开会", 2))
    }

    @Test
    fun questionAsksPrimarySlotOnly() {
        val turn = StructuredTurn(
            intent = TurnIntent.Clarify,
            timeExplicit = false,
            clarifyQuestion = "请补充时间地点和参会人好吗？",
            missingSlots = listOf("people", "place", "time"),
            entries = listOf(entry(diaryType = "meeting")),
        )
        assertEquals(listOf("time", "place", "people"), Sufficiency.missingSlots(turn))
        assertEquals("什么时候？", Sufficiency.question(turn))
    }

    @Test
    fun declaredSlotsWinForMeeting() {
        val turn = StructuredTurn(
            intent = TurnIntent.Record,
            timeExplicit = true,
            missingSlots = listOf("place", "people"),
            entries = listOf(entry(diaryType = "meeting", place = "x", people = listOf("y"))),
        )
        assertEquals(listOf("place", "people"), Sufficiency.missingSlots(turn))
        assertTrue(Sufficiency.shouldClarify(turn, "开会", 0))
    }

    @Test
    fun mutateNeverClarifies() {
        val turn = StructuredTurn(
            intent = TurnIntent.Mutate,
            mutateAction = "delete",
            timeExplicit = false,
            missingSlots = listOf("time", "place", "people"),
            entries = listOf(entry(diaryType = "meeting")),
        )
        assertFalse(Sufficiency.shouldClarify(turn, "删掉明天的会", 0))
    }

    private fun entry(
        title: String = "开会",
        diaryType: String = "meeting",
        place: String = "",
        people: List<String> = emptyList(),
        body: String = "开会",
    ) = StructuredEntry(
        title = title,
        startAt = "2026-08-19T15:00:00+08:00",
        place = place,
        people = people,
        diaryType = diaryType,
        body = body,
        raw = title,
    )
}
