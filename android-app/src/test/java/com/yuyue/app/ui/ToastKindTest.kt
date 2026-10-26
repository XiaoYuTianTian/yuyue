package com.yuyue.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ToastKindTest {
    @Test
    fun kindsAreNoneUndoOpenEntry() {
        assertEquals(
            listOf(ToastKind.None, ToastKind.Undo, ToastKind.OpenEntry),
            ToastKind.entries.toList(),
        )
    }
}
