package com.yuyue.app.capture

import com.yuyue.app.data.capture.CaptureCommand
import com.yuyue.app.data.capture.CaptureEntryWriter
import com.yuyue.app.data.capture.CaptureIngress
import com.yuyue.app.data.capture.CaptureKernel
import com.yuyue.app.data.capture.CaptureKernelStatus
import com.yuyue.app.data.capture.CaptureMutateGateway
import com.yuyue.app.data.capture.CapturePhase
import com.yuyue.app.data.capture.CapturePolishFollowUp
import com.yuyue.app.data.capture.CaptureRawStore
import com.yuyue.app.data.capture.CaptureStatus
import com.yuyue.app.data.capture.InteractiveKind
import com.yuyue.app.data.capture.MaterializePayload
import com.yuyue.app.data.capture.PolishFollowUpResult
import com.yuyue.app.data.capture.PrivacyCaptureEvent
import com.yuyue.app.llm.RouteKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.UUID

class CaptureKernelTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val capturedAt = 1_725_000_000_000L

    @Test
    fun offlineRecordCreatesEntryWithinBudget() = runBlocking {
        var now = 1_000L
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val polish = FakePolish(fail = true)
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            polishFollowUp = polish,
            clock = { now },
            idGenerator = { "req-record" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "记下今天下午喝了咖啡",
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
                notebookName = "生活",
            ),
        )
        now += 50L

        assertEquals(RouteKind.Record, outcome.route)
        assertEquals(CaptureKernelStatus.LocalSaved, outcome.status)
        assertEquals(CapturePhase.Completed, outcome.phase)
        assertEquals(1, writer.payloads.size)
        assertEquals(1, writer.payloads.single().entries.size)
        assertTrue(outcome.entryIds.isNotEmpty())
        assertTrue(outcome.localPersistMs <= 300L)
        assertEquals(1, polish.calls)
        assertTrue(raw.rows["req-record"]!!.status == CaptureStatus.Completed ||
            raw.rows["req-record"]!!.status == CaptureStatus.Retryable)
    }

    @Test
    fun emptyVoiceZeroWrite() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            idGenerator = { "req-empty" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "   ",
                source = CaptureIngress.Voice,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
                framesHeard = true,
            ),
        )

        assertEquals(CaptureKernelStatus.Rejected, outcome.status)
        assertEquals(CapturePhase.Rejected, outcome.phase)
        assertTrue(writer.payloads.isEmpty())
        assertTrue(raw.rows.isEmpty())
        assertTrue(outcome.entryIds.isEmpty())
    }

    @Test
    fun recallZeroWrite() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            idGenerator = { "req-recall" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "上周喝咖啡那次是哪天",
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
            ),
        )

        assertEquals(RouteKind.Recall, outcome.route)
        assertEquals(CaptureKernelStatus.ZeroWrite, outcome.status)
        assertEquals(InteractiveKind.Recall, outcome.interactiveKind)
        assertTrue(writer.payloads.isEmpty())
        assertTrue(outcome.entryIds.isEmpty())
        assertEquals(CaptureStatus.ForegroundRequired, raw.rows["req-recall"]!!.status)
    }

    @Test
    fun mutateUniqueHitAppliesWithoutNewDiary() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val mutate = FakeMutate(candidates = listOf("ev-1"))
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            mutateGateway = mutate,
            idGenerator = { "req-mutate" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "把昨天那条删掉",
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
            ),
        )

        assertEquals(RouteKind.Mutate, outcome.route)
        assertEquals(CaptureKernelStatus.Completed, outcome.status)
        assertEquals(listOf("ev-1"), mutate.applied)
        assertTrue(writer.payloads.isEmpty())
        assertTrue(outcome.entryIds.isEmpty())
    }

    @Test
    fun mutateNeedsPickWhenAmbiguous() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val mutate = FakeMutate(candidates = listOf("ev-1", "ev-2"))
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            mutateGateway = mutate,
            idGenerator = { "req-pick" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "把那条删掉",
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
            ),
        )

        assertEquals(CaptureKernelStatus.NeedsInteraction, outcome.status)
        assertEquals(InteractiveKind.MutatePick, outcome.interactiveKind)
        assertEquals(listOf("ev-1", "ev-2"), outcome.candidateEventIds)
        assertTrue(mutate.applied.isEmpty())
        assertTrue(writer.payloads.isEmpty())
    }

    @Test
    fun mutateUsesSelectedEventId() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val mutate = FakeMutate(candidates = listOf("ev-1", "ev-2"))
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            mutateGateway = mutate,
            idGenerator = { "req-sel" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "把那条删掉",
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
                selectedEventId = "ev-2",
            ),
        )

        assertEquals(CaptureKernelStatus.Completed, outcome.status)
        assertEquals(listOf("ev-2"), mutate.applied)
        assertTrue(writer.payloads.isEmpty())
    }

    @Test
    fun multiSplitCreatesMultipleEntries() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            idGenerator = { "req-split" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "明早买菜，下午跑步",
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
            ),
        )

        assertEquals(CaptureKernelStatus.LocalSaved, outcome.status)
        assertEquals(2, writer.payloads.single().entries.size)
        assertEquals(2, outcome.entryIds.size)
    }

    @Test
    fun fastExtractFailureStillLocalSucceeds() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val polish = FakePolish(fail = true, httpStatus = 402)
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            polishFollowUp = polish,
            idGenerator = { "req-402" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "记下今天开会",
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
            ),
        )

        assertEquals(CaptureKernelStatus.LocalSaved, outcome.status)
        assertEquals(1, writer.payloads.size)
        assertTrue(outcome.offlineStatusMessage != null)
        assertEquals(1, polish.calls)
        val polishEvent = outcome.privacyEvents.last { it.phase == CapturePhase.OptionalPolish }
        assertEquals(402, polishEvent.httpStatus)
    }

    @Test
    fun questionDoesNotCreateDiary() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            idGenerator = { "req-q" },
        )

        val outcome = kernel.accept(
            CaptureCommand(
                text = "今天天气怎么样？",
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
            ),
        )

        assertEquals(CaptureKernelStatus.ZeroWrite, outcome.status)
        assertTrue(writer.payloads.isEmpty())
        assertTrue(outcome.entryIds.isEmpty())
    }

    @Test
    fun privacyLogNeverContainsTitleBodyOrRaw() = runBlocking {
        val raw = FakeRawStore()
        val writer = FakeEntryWriter()
        val polish = FakePolish(fail = true, httpStatus = 402, modelId = "qwen-test")
        val kernel = CaptureKernel(
            rawStore = raw,
            entryWriter = writer,
            polishFollowUp = polish,
            idGenerator = { "req-priv" },
        )
        val secret = "秘密标题咖啡正文原话"

        val outcome = kernel.accept(
            CaptureCommand(
                text = secret,
                source = CaptureIngress.Keyboard,
                capturedAtMillis = capturedAt,
                timezone = zone,
                notebookId = "nb-1",
                notebookName = "生活",
            ),
        )

        val joined = outcome.privacyEvents.joinToString("\n") { it.toLogLine() }
        assertFalse(joined.contains(secret))
        assertFalse(joined.contains("秘密"))
        assertFalse(joined.contains("咖啡"))
        assertTrue(joined.contains("req-priv"))
        assertTrue(joined.contains("phase="))
        outcome.privacyEvents.forEach { ev ->
            assertEquals("req-priv", ev.requestId)
            assertTrue(ev.modelId == null || ev.modelId == "qwen-test")
        }
    }

    private class FakeRawStore : CaptureRawStore {
        val rows = linkedMapOf<String, RawRow>()

        override suspend fun saveRaw(command: CaptureCommand, requestId: String) {
            rows[requestId] = RawRow(
                requestId = requestId,
                raw = command.text,
                status = CaptureStatus.Pending,
                phase = CapturePhase.Accepted,
                interactiveKind = null,
                message = null,
            )
        }

        override suspend fun updateMeta(
            requestId: String,
            status: CaptureStatus,
            phase: CapturePhase,
            interactiveKind: InteractiveKind?,
            message: String?,
        ) {
            val cur = rows[requestId] ?: return
            rows[requestId] = cur.copy(
                status = status,
                phase = phase,
                interactiveKind = interactiveKind,
                message = message,
            )
        }
    }

    private data class RawRow(
        val requestId: String,
        val raw: String,
        val status: CaptureStatus,
        val phase: CapturePhase,
        val interactiveKind: InteractiveKind?,
        val message: String?,
    )

    private class FakeEntryWriter : CaptureEntryWriter {
        val payloads = mutableListOf<MaterializePayload>()
        override suspend fun writeRecord(payload: MaterializePayload): List<String> {
            payloads += payload
            return payload.entries.map { UUID.randomUUID().toString() }
        }
    }

    private class FakeMutate(
        private val candidates: List<String>,
    ) : CaptureMutateGateway {
        val applied = mutableListOf<String>()
        override suspend fun findCandidates(notebookId: String, text: String): List<String> = candidates
        override suspend fun applyMutate(eventId: String, text: String) {
            applied += eventId
        }
    }

    private class FakePolish(
        private val fail: Boolean,
        private val httpStatus: Int? = null,
        private val modelId: String? = null,
    ) : CapturePolishFollowUp {
        var calls = 0
        override suspend fun tryPolish(
            requestId: String,
            rawTranscript: String,
            notebookId: String,
            notebookName: String,
            mediaPaths: List<String>,
        ): PolishFollowUpResult {
            calls += 1
            return PolishFollowUpResult(
                succeeded = !fail,
                httpStatus = httpStatus,
                modelId = modelId,
            )
        }
    }
}
