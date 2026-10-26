package com.yuyue.app.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DonePolicyTest {
    @Test
    fun pastIsDone() {
        assertTrue(DonePolicy.isDone(1_000L, 2_000L))
    }

    @Test
    fun nowIsDone() {
        assertTrue(DonePolicy.isDone(2_000L, 2_000L))
    }

    @Test
    fun futureIsPending() {
        assertFalse(DonePolicy.isDone(3_000L, 2_000L))
    }
}
