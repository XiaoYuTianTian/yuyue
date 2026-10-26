package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredTurnParserTest {
    @Test
    fun parsesBareJsonRecord() {
        val turn = StructuredTurnParser.parse(
            """
            {"intent":"record","need_more":false,"clarify_question":null,
             "entries":[{"title":"喝了咖啡","start_at":"2026-08-17T15:30:00+08:00",
             "place":"","people":[],"diary_type":"food","body":"下班喝了咖啡","raw":"下了班去喝了咖啡"}],
             "recall_query":null}
            """.trimIndent(),
        )
        assertEquals(TurnIntent.Record, turn.intent)
        assertFalse(turn.needMore)
        assertEquals(1, turn.entries.size)
        assertEquals("喝了咖啡", turn.entries[0].title)
        assertEquals("food", turn.entries[0].diaryType)
    }

    @Test
    fun parsesFencedJson() {
        val turn = StructuredTurnParser.parse(
            """
            ```json
            {"intent":"record","need_more":false,"entries":[
              {"title":"开会","start_at":"2026-08-18T15:00:00+08:00","place":"",
               "people":["客户"],"diary_type":"meeting","body":"见客户","raw":"明天下午三点见客户"}
            ]}
            ```
            """.trimIndent(),
        )
        assertEquals(TurnIntent.Record, turn.intent)
        assertEquals("开会", turn.entries.single().title)
    }

    @Test
    fun parsesTwoEntries() {
        val turn = StructuredTurnParser.parse(
            """
            {"intent":"record","need_more":false,"entries":[
              {"title":"午饭","start_at":"2026-08-17T12:00:00+08:00","place":"","people":[],"diary_type":"food","body":"午饭","raw":"中午吃饭"},
              {"title":"散步","start_at":"2026-08-17T18:00:00+08:00","place":"公园","people":[],"diary_type":"life","body":"散步","raw":"晚上散步"}
            ]}
            """.trimIndent(),
        )
        assertEquals(2, turn.entries.size)
        assertEquals("午饭", turn.entries[0].title)
        assertEquals("散步", turn.entries[1].title)
        assertEquals("公园", turn.entries[1].place)
    }

    @Test
    fun parsesClarifyWithDraft() {
        val turn = StructuredTurnParser.parse(
            """{"intent":"clarify","need_more":true,"clarify_question":"什么时候？",
                "time_explicit":false,"missing_slots":["time","place"],
                "entries":[{"title":"开会","start_at":"2026-08-19T15:00:00+08:00","place":"","people":[],"diary_type":"meeting","body":"开会","raw":"要开会"}]}""",
        )
        assertEquals(TurnIntent.Clarify, turn.intent)
        assertTrue(turn.needMore)
        assertEquals("什么时候？", turn.clarifyQuestion)
        assertFalse(turn.timeExplicit)
        assertEquals(listOf("time", "place"), turn.missingSlots)
        assertEquals("开会", turn.entries.single().title)
    }

    @Test(expected = StructuredTurnParseException::class)
    fun invalidJsonThrows() {
        StructuredTurnParser.parse("这不是 JSON")
    }

    @Test
    fun recallQueryOptional() {
        val turn = StructuredTurnParser.parse(
            """{"intent":"recall","need_more":false,"entries":[],
                "recall_query":{"time_from":"2026-08-10","time_to":"2026-08-17","diary_type":"food","keyword":"咖啡","title_hint":null}}""",
        )
        assertEquals(TurnIntent.Recall, turn.intent)
        assertEquals("咖啡", turn.recallQuery?.keyword)
        assertNull(turn.recallQuery?.titleHint)
    }

    @Test
    fun parsesMutateIntent() {
        val turn = StructuredTurnParser.parse(
            """{"intent":"mutate","need_more":false,"mutate_action":"reschedule",
                "target_hint":{"keyword":"开会","title_hint":null,"time_from":"2026-08-22","time_to":"2026-08-22"},
                "entries":[],"recall_query":null}""",
        )
        assertEquals(TurnIntent.Mutate, turn.intent)
        assertEquals("reschedule", turn.mutateAction)
        assertEquals("开会", turn.targetHint?.keyword)
        assertEquals("2026-08-22", turn.targetHint?.timeFrom)
    }

    @Test(expected = StructuredTurnParseException::class)
    fun missingIntentThrows() {
        StructuredTurnParser.parse("""{"need_more":false,"entries":[]}""")
    }

    @Test
    fun proseAroundFencedJsonStillParses() {
        val turn = StructuredTurnParser.parse(
            """
            好的，整理如下：
            ```json
            {"intent":"record","need_more":false,"entries":[
              {"title":"开会","start_at":"2026-08-18T15:00:00+08:00","place":"",
               "people":[],"diary_type":"meeting","body":"见客户","raw":"明天下午三点见客户"}
            ]}
            ```
            另外我还可以再补细节。
            """.trimIndent(),
        )
        assertEquals(TurnIntent.Record, turn.intent)
        assertEquals("开会", turn.entries.single().title)
    }
}
