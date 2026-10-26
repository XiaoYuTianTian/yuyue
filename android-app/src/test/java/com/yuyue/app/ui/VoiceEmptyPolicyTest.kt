package com.yuyue.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceEmptyPolicyTest {
    @Test
    fun heardButEmptyAsksRetry() {
        assertEquals("没听清，请再按住说一次", VoiceEmptyPolicy.message("", framesHeard = true))
    }

    @Test
    fun silenceSaysNoSound() {
        assertEquals("没有听到声音", VoiceEmptyPolicy.message("  ", framesHeard = false))
    }

    @Test
    fun transcriptKeepsDraft() {
        assertNull(VoiceEmptyPolicy.message("今天下午喝了咖啡", framesHeard = true))
    }
}
