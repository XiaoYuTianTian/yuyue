package com.yuyue.app.capture

import com.yuyue.app.data.capture.CaptureQueueItem
import com.yuyue.app.data.capture.CaptureStatus
import com.yuyue.app.data.capture.isEntryPolishing
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureQueueModelsTest {
    @Test
    fun polishingMatchesRequestPrefix() {
        val queue = listOf(
            CaptureQueueItem(
                requestId = "req-a",
                rawTranscript = "喝了杯咖啡",
                status = CaptureStatus.Processing,
                lastError = "正在润色",
                capturedAtMillis = 1L,
                attemptCount = 1,
            ),
        )
        assertTrue(queue.isEntryPolishing("req-a:0"))
        assertFalse(queue.isEntryPolishing("req-b:0"))
        assertFalse(queue.isEntryPolishing("uuid-without-colon"))
    }
}
