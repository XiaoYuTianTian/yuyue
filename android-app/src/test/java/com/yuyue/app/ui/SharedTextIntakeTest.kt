package com.yuyue.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedTextIntakeTest {
    @Test
    fun holdsWhenNotebookMissing() {
        val r = SharedTextIntake.decide(notebookId = null, text = "记下今天下午喝了咖啡")
        assertTrue(r is SharedTextIntake.Hold)
        assertEquals("记下今天下午喝了咖啡", (r as SharedTextIntake.Hold).text)
    }

    @Test
    fun submitsWhenNotebookReady() {
        val r = SharedTextIntake.decide(notebookId = "nb-1", text = "记下今天下午喝了咖啡")
        assertTrue(r is SharedTextIntake.Submit)
    }

    @Test
    fun blankIsIgnore() {
        assertTrue(SharedTextIntake.decide("nb-1", "  ") is SharedTextIntake.Ignore)
    }
}
