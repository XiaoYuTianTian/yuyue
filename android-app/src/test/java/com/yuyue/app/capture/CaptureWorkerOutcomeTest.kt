package com.yuyue.app.capture

import com.yuyue.app.data.capture.CaptureWorkResult
import com.yuyue.app.data.capture.CaptureWorkerOutcome
import com.yuyue.app.data.capture.ProcessResult
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureWorkerOutcomeTest {
    @Test
    fun missingRequestIdSucceedsWithoutProcessing() {
        assertEquals(
            CaptureWorkResult.Success,
            CaptureWorkerOutcome.of(requestId = null, appAvailable = true, process = null),
        )
    }

    @Test
    fun nonYuyueApplicationRetries() {
        assertEquals(
            CaptureWorkResult.Retry,
            CaptureWorkerOutcome.of(requestId = "req-1", appAvailable = false, process = ProcessResult.Completed),
        )
    }

    @Test
    fun completedForegroundRequiredAndSkippedSucceed() {
        assertEquals(
            CaptureWorkResult.Success,
            CaptureWorkerOutcome.of("req-1", true, ProcessResult.Completed),
        )
        assertEquals(
            CaptureWorkResult.Success,
            CaptureWorkerOutcome.of("req-1", true, ProcessResult.ForegroundRequired),
        )
        assertEquals(
            CaptureWorkResult.Success,
            CaptureWorkerOutcome.of("req-1", true, ProcessResult.Skipped),
        )
    }

    @Test
    fun retryableProcessRetriesWork() {
        assertEquals(
            CaptureWorkResult.Retry,
            CaptureWorkerOutcome.of("req-1", true, ProcessResult.Retryable),
        )
    }
}
