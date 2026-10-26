package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastExtractParserTest {
    @Test
    fun unsureIntentBecomesRecord() {
        val result = FastExtractParser.parse(
            rawJson = """{"intent":"maybe","time_explicit":false,"start_at":"","title":"喝咖啡","body":"楼下点了一杯","raw":"喝了杯咖啡"}""",
            nowIso = "2026-08-22T18:00:00+08:00",
            userText = "喝了杯咖啡",
        )
        assertEquals(TurnIntent.Record, result.intent)
        assertFalse(result.timeExplicit)
        assertEquals("2026-08-22T18:00:00+08:00", result.entries.single().startAt)
        assertEquals("喝咖啡", result.entries.single().title)
    }

    @Test
    fun explicitDateKeepsStartAt() {
        val result = FastExtractParser.parse(
            rawJson = """{"intent":"record","time_explicit":true,"start_at":"2026-08-26T15:00:00+08:00","title":"见客户","body":"明天下午见面","raw":"明天下午见客户"}""",
            nowIso = "2026-08-22T18:00:00+08:00",
            userText = "明天下午见客户",
        )
        assertTrue(result.timeExplicit)
        assertEquals("2026-08-26T15:00:00+08:00", result.entries.single().startAt)
    }

    @Test
    fun recallDoesNotInventEntry() {
        val result = FastExtractParser.parse(
            rawJson = """{"intent":"recall","time_explicit":true,"start_at":"2026-08-15T10:00:00+08:00","title":"","body":"","raw":"上周那次咖啡哪天"}""",
            nowIso = "2026-08-22T18:00:00+08:00",
            userText = "上周那次咖啡哪天",
        )
        assertEquals(TurnIntent.Recall, result.intent)
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun recallWordingOverridesWeakRecordJson() {
        val result = FastExtractParser.parse(
            rawJson = """{"intent":"record","time_explicit":false,"title":"咖啡","body":"上周那次","raw":"上周喝咖啡那次是哪天"}""",
            nowIso = "2026-08-22T18:00:00+08:00",
            userText = "上周喝咖啡那次是哪天",
        )
        assertEquals(TurnIntent.Recall, result.intent)
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun mutateWordingOverridesMissingIntent() {
        val result = FastExtractParser.parse(
            rawJson = """{"intent":"maybe","title":"昨天","body":"删掉","raw":"把昨天那条删掉"}""",
            nowIso = "2026-08-22T18:00:00+08:00",
            userText = "把昨天那条删掉",
        )
        assertEquals(TurnIntent.Mutate, result.intent)
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun extraProseAroundJsonStillParses() {
        val result = FastExtractParser.parse(
            rawJson = "好的，这是结果：\n{\"intent\":\"record\",\"time_explicit\":false,\"start_at\":\"\",\"title\":\"喝水\",\"body\":\"喝了水\",\"raw\":\"喝了水\"}\n谢谢",
            nowIso = "2026-08-22T18:00:00+08:00",
            userText = "喝了水",
        )
        assertEquals(TurnIntent.Record, result.intent)
        assertEquals("喝水", result.entries.single().title)
    }

    @Test
    fun commaSeparatedEventsBecomeTwoEntries() {
        val result = FastExtractParser.parse(
            rawJson = """{"intent":"record","time_explicit":true,"start_at":"2026-08-23T09:00:00+08:00","title":"买菜跑步","body":"明早买菜，下午跑步","raw":"明早买菜，下午跑步"}""",
            nowIso = "2026-08-22T18:00:00+08:00",
            userText = "明早买菜，下午跑步",
        )
        assertEquals(2, result.entries.size)
        assertTrue(result.entries[0].startAt.startsWith("2026-08-23T09:00:00"))
        assertTrue(result.entries[1].startAt.startsWith("2026-08-23T15:00:00"))
        assertTrue("买菜" in result.entries[0].title || "买菜" in result.entries[0].body)
        assertTrue("跑步" in result.entries[1].title || "跑步" in result.entries[1].body)
    }

    @Test
    fun photoOnlyUsesNowAndPhotoTitle() {
        val result = FastExtractParser.photoOnly("2026-08-22T18:01:00+08:00")
        assertEquals(TurnIntent.Record, result.intent)
        assertFalse(result.timeExplicit)
        assertEquals("照片", result.entries.single().title)
        assertEquals("2026-08-22T18:01:00+08:00", result.entries.single().startAt)
    }
}
