package com.yuyue.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureLaunchTest {
    @Test
    fun recognizesQuickCaptureAction() {
        assertTrue(CaptureLaunch.isQuickCapture(CaptureLaunch.QUICK_ACTION))
        assertFalse(CaptureLaunch.isQuickCapture("android.intent.action.SEND"))
        assertFalse(CaptureLaunch.isQuickCapture(null))
    }

    @Test
    fun quickCaptureOpensVoiceNotKeyboard() {
        assertTrue(CaptureLaunch.opensVoiceBall(CaptureLaunch.QUICK_ACTION))
        assertFalse(CaptureLaunch.opensVoiceBall("android.intent.action.SEND"))
    }
}
