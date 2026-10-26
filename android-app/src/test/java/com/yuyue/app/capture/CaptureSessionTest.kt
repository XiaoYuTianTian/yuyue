package com.yuyue.app.capture

import com.yuyue.app.data.capture.CaptureQueueItem
import com.yuyue.app.data.capture.CaptureSession
import com.yuyue.app.data.capture.CaptureStatus
import com.yuyue.app.data.capture.InteractiveKind
import com.yuyue.app.data.capture.SubmitAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSessionTest {
    private fun waitingClarify(
        requestId: String = "req-a",
        raw: String = "明天下午开会",
        lastError: String = "需要补充信息：什么时候？",
    ) = CaptureQueueItem(
        requestId = requestId,
        rawTranscript = raw,
        status = CaptureStatus.ForegroundRequired,
        lastError = lastError,
        capturedAtMillis = 10L,
        attemptCount = 1,
    )

    @Test
    fun clarifyFollowUpContinuesSameRequestAndAppends() {
        val action = CaptureSession.resolve(
            pendingRequestId = "req-a",
            pendingMutateRequestId = null,
            queue = listOf(waitingClarify()),
            existingRaw = "明天下午开会",
            newText = "三点",
        )
        val cont = action as SubmitAction.Continue
        assertEquals("req-a", cont.requestId)
        assertEquals(InteractiveKind.RecordClarify, cont.kind)
        assertEquals("明天下午开会\n补充：三点", cont.combinedRaw)
    }

    @Test
    fun queueWaitingWithoutMemoryStillContinues() {
        val action = CaptureSession.resolve(
            pendingRequestId = null,
            pendingMutateRequestId = null,
            queue = listOf(waitingClarify()),
            existingRaw = null,
            newText = "在公司",
        )
        val cont = action as SubmitAction.Continue
        assertEquals("req-a", cont.requestId)
        assertTrue(cont.combinedRaw.endsWith("补充：在公司"))
    }

    @Test
    fun afterCancelNewCaptureOpens() {
        val action = CaptureSession.resolve(
            pendingRequestId = null,
            pendingMutateRequestId = null,
            queue = emptyList(),
            existingRaw = null,
            newText = "喝了杯咖啡",
        )
        assertEquals(SubmitAction.NewCapture, action)
    }

    @Test
    fun recallFollowUpDoesNotOpenNewCapture() {
        val action = CaptureSession.resolve(
            pendingRequestId = null,
            pendingMutateRequestId = null,
            queue = listOf(
                waitingClarify(
                    requestId = "req-r",
                    raw = "上周那次咖啡哪天",
                    lastError = "回忆缺少线索：还记得哪一天吗？",
                ),
            ),
            existingRaw = null,
            newText = "周三那次",
        )
        val cont = action as SubmitAction.Continue
        assertEquals("req-r", cont.requestId)
        assertEquals(InteractiveKind.Recall, cont.kind)
        assertEquals("上周那次咖啡哪天\n补充：周三那次", cont.combinedRaw)
    }

    @Test
    fun questionAndKindRoundTrip() {
        val reason = CaptureSession.formatForegroundReason(
            InteractiveKind.RecordClarify,
            "什么时候？",
        )
        assertEquals("需要补充信息：什么时候？", reason)
        assertEquals(InteractiveKind.RecordClarify, CaptureSession.kindFromLastError(reason))
        assertEquals("什么时候？", CaptureSession.questionFromLastError(reason))
        assertNull(CaptureSession.kindFromLastError("正在润色"))
    }

    @Test
    fun splitAndRebuildHistory() {
        val combined = CaptureSession.appendSupplement(
            CaptureSession.appendSupplement("开会", "三点"),
            "在公司",
        )
        assertEquals(listOf("开会", "三点", "在公司"), CaptureSession.splitSupplements(combined))
        val history = CaptureSession.rebuildTurnHistory(combined, "在哪里？")
        assertEquals(5, history.size)
        assertEquals("开会", history[0].content)
        assertEquals("assistant", history[1].role)
        assertEquals("三点", history[2].content)
        assertEquals("在公司", history[4].content)
    }
}
