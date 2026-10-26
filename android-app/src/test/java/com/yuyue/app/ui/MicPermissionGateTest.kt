package com.yuyue.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicPermissionGateTest {
    @Test
    fun promptsWhenNotGranted() {
        assertTrue(MicPermissionGate.shouldPrompt(granted = false))
        assertFalse(MicPermissionGate.shouldPrompt(granted = true))
    }
}
