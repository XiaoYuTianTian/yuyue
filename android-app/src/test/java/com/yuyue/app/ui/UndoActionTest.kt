package com.yuyue.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoActionTest {
    @Test
    fun toggleDoneStoresPrevious() {
        val action = UndoAction.ToggleDone("e1", previous = false)
        assertEquals("e1", action.id)
        assertEquals(false, action.previous)
    }

    @Test
    fun softDeleteKeepsId() {
        val action = UndoAction.SoftDelete("e2")
        assertEquals("e2", action.id)
        assertTrue(action is UndoAction.SoftDelete)
    }
}
