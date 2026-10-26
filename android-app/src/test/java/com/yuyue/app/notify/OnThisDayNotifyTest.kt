package com.yuyue.app.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnThisDayNotifyTest {
    @Test
    fun buildsTitleAndBodyFromEntries() {
        val content = OnThisDayNotify.build(
            listOf(
                OnThisDayNotify.Hit(year = 2024, title = "咖啡"),
                OnThisDayNotify.Hit(year = 2023, title = "散步"),
            ),
        )
        assertEquals("那年今日", content!!.title)
        assertTrue(content.body.contains("咖啡"))
        assertTrue(content.body.contains("2024"))
        assertTrue(content.body.contains("2"))
    }

    @Test
    fun emptyHitsSkipNotification() {
        assertNull(OnThisDayNotify.build(emptyList()))
    }

    @Test
    fun singleHitUsesTitleDirectly() {
        val content = OnThisDayNotify.build(
            listOf(OnThisDayNotify.Hit(year = 2022, title = "第一次见面")),
        )
        assertEquals("那年今日", content!!.title)
        assertTrue(content.body.contains("2022"))
        assertTrue(content.body.contains("第一次见面"))
    }
}
