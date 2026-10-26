package com.yuyue.app.capture

import com.yuyue.app.data.capture.CaptureLedgerEngine
import com.yuyue.app.data.capture.CaptureLedgerRecord
import com.yuyue.app.data.capture.CaptureLedgerStore
import com.yuyue.app.data.capture.CaptureStatus
import com.yuyue.app.data.capture.EntryLocator
import com.yuyue.app.data.capture.EntryMaterializer
import com.yuyue.app.data.capture.ExtractGateway
import com.yuyue.app.data.capture.MaterializePayload
import com.yuyue.app.data.capture.PolishGateway
import com.yuyue.app.data.capture.ProcessResult
import com.yuyue.app.data.capture.ProcessingMode
import com.yuyue.app.data.capture.TurnGateway
import com.yuyue.app.llm.FastExtractResult
import com.yuyue.app.llm.LockedPolishEntry
import com.yuyue.app.llm.StructuredEntry
import com.yuyue.app.llm.StructuredTurn
import com.yuyue.app.llm.TurnIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureLedgerEngineTest {
    @Test
    fun backgroundMutateDoesNotApplyMutation() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-1",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "把昨天那条删掉",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        val parser = FakeTurnGateway(
            StructuredTurn(
                intent = TurnIntent.Mutate,
                mutateAction = "delete",
            ),
        )
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = parser,
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
        )

        val result = engine.process("req-1", ProcessingMode.Background)

        assertEquals(ProcessResult.ForegroundRequired, result)
        assertTrue(entries.calls.isEmpty())
        assertEquals(CaptureStatus.ForegroundRequired, store.get("req-1")!!.status)
    }

    @Test
    fun recordUsesStableSourceAssociationAndIsIdempotent() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-2",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "明早买菜，下午跑步",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        val parser = FakeTurnGateway(
            StructuredTurn(
                intent = TurnIntent.Record,
                timeExplicit = false,
                entries = listOf(
                    StructuredEntry(
                        title = "买菜",
                        startAt = "2026-08-22T09:00:00+08:00",
                        body = "买水果",
                        raw = "",
                    ),
                    StructuredEntry(
                        title = "跑步",
                        startAt = "2026-08-22T16:00:00+08:00",
                        body = "跑五公里",
                        raw = "",
                    ),
                ),
            ),
        )
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = parser,
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
        )

        engine.process("req-2", ProcessingMode.Foreground)
        engine.process("req-2", ProcessingMode.Foreground)

        assertEquals(1, entries.calls.size)
        val payload = entries.calls.single()
        assertEquals("req-2", payload.requestId)
        assertEquals("req-2", payload.utteranceId)
        assertTrue(payload.entries.all { it.rawTranscript == "明早买菜，下午跑步" })
        assertEquals(CaptureStatus.Completed, store.get("req-2")!!.status)
    }

    @Test
    fun missingKeyKeepsRequestRetryable() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-3",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "记一下今天的工作",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        val parser = FakeTurnGateway(
            StructuredTurn(
                intent = TurnIntent.Record,
                entries = listOf(
                    StructuredEntry(
                        title = "工作",
                        startAt = "2026-08-22T10:00:00+08:00",
                        body = "整理需求",
                        raw = "",
                    ),
                ),
            ),
        )
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = parser,
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { false },
            isNetworkAvailable = { true },
        )

        val result = engine.process("req-3", ProcessingMode.Background)

        assertEquals(ProcessResult.Completed, result)
        assertEquals(1, entries.calls.size)
        assertEquals(CaptureStatus.Completed, store.get("req-3")!!.status)
    }

    @Test
    fun pausedCaptureIsSkippedWithoutChangingStatus() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-paused",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "先暂停这条",
                status = CaptureStatus.Paused,
                capturedAtMillis = 1_000L,
                lastError = "已暂停",
            ),
        )
        val parser = FakeTurnGateway(
            StructuredTurn(
                intent = TurnIntent.Record,
                entries = listOf(
                    StructuredEntry(
                        title = "不应处理",
                        startAt = "2026-08-22T10:00:00+08:00",
                        body = "不应落盘",
                        raw = "",
                    ),
                ),
            ),
        )
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = parser,
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
        )

        val result = engine.process("req-paused", ProcessingMode.Background)

        assertEquals(ProcessResult.Skipped, result)
        assertTrue(entries.calls.isEmpty())
        assertEquals(CaptureStatus.Paused, store.get("req-paused")!!.status)
        assertEquals("已暂停", store.get("req-paused")!!.lastError)
    }

    @Test
    fun offlineRecordMaterializesWithoutCallingCloud() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-4",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "记下刚才出门了",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        var cloudCalls = 0
        val parser = object : TurnGateway {
            override suspend fun complete(
                rawTranscript: String,
                notebookId: String,
                notebookName: String,
                imagePaths: List<String>,
            ): StructuredTurn {
                cloudCalls++
                error("should not call cloud offline")
            }
        }
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = parser,
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { false },
        )

        val result = engine.process("req-4", ProcessingMode.Foreground)

        assertEquals(ProcessResult.Completed, result)
        assertEquals(0, cloudCalls)
        assertEquals(1, entries.calls.size)
        assertEquals("记下刚才出门了", entries.calls.single().entries.single().rawTranscript)
    }

    @Test
    fun offlinePhotoDoesNotUseLocalFallback() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-photo",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
                mediaJson = """[{"id":"m1","path":"/tmp/a.jpg","thumbPath":"/tmp/a_t.jpg"}]""",
            ),
        )
        var cloudCalls = 0
        val parser = object : TurnGateway {
            override suspend fun complete(
                rawTranscript: String,
                notebookId: String,
                notebookName: String,
                imagePaths: List<String>,
            ): StructuredTurn {
                cloudCalls++
                error("should not call cloud offline")
            }
        }
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = parser,
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { false },
        )

        val result = engine.process("req-photo", ProcessingMode.Foreground)

        assertEquals(ProcessResult.Retryable, result)
        assertEquals(0, cloudCalls)
        assertTrue(entries.calls.isEmpty())
        assertEquals(CaptureStatus.Retryable, store.get("req-photo")!!.status)
    }

    @Test
    fun extractRecordPersistsThenPolishes() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-extract",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "明天下午见客户",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        var cloudCalls = 0
        val parser = object : TurnGateway {
            override suspend fun complete(
                rawTranscript: String,
                notebookId: String,
                notebookName: String,
                imagePaths: List<String>,
            ): StructuredTurn {
                cloudCalls++
                error("large model should wait for polish")
            }
        }
        val entries = FakeEntryMaterializer()
        var polishCalls = 0
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = parser,
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
            extractGateway = object : ExtractGateway {
                override suspend fun extract(
                    rawTranscript: String,
                    notebookName: String,
                    nowIso: String,
                    hasImages: Boolean,
                ) = FastExtractResult(
                    intent = TurnIntent.Record,
                    timeExplicit = true,
                    entries = listOf(
                        StructuredEntry(
                            title = "见客户",
                            startAt = "2026-08-23T15:00:00+08:00",
                            body = "明天下午见面",
                            raw = rawTranscript,
                        ),
                    ),
                )
            },
            polishGateway = object : PolishGateway {
                override suspend fun polish(
                    requestId: String,
                    rawTranscript: String,
                    notebookId: String,
                    notebookName: String,
                    locked: List<LockedPolishEntry>,
                    imagePaths: List<String>,
                    timeExplicit: Boolean,
                ) {
                    polishCalls++
                    assertTrue(timeExplicit)
                }
            },
            entryLocator = object : EntryLocator {
                override suspend fun listForRequest(requestId: String): List<LockedPolishEntry> {
                    if (entries.calls.isEmpty()) return emptyList()
                    return listOf(
                        LockedPolishEntry(
                            entryId = "$requestId:0",
                            startAtIso = "2026-08-23T15:00:00+08:00",
                            startAtMillis = 1_777_000_000_000L,
                        ),
                    )
                }
            },
        )

        val result = engine.process("req-extract", ProcessingMode.Background)

        assertEquals(ProcessResult.Completed, result)
        assertEquals(0, cloudCalls)
        assertEquals(1, entries.calls.size)
        assertEquals(1, polishCalls)
        assertEquals(CaptureStatus.Completed, store.get("req-extract")!!.status)
    }

    @Test
    fun extractRecallDoesNotPersist() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-recall",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "上周那次咖啡哪天",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        var cloudCalls = 0
        val parser = object : TurnGateway {
            override suspend fun complete(
                rawTranscript: String,
                notebookId: String,
                notebookName: String,
                imagePaths: List<String>,
            ): StructuredTurn {
                cloudCalls++
                return StructuredTurn(intent = TurnIntent.Recall)
            }
        }
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = parser,
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
            extractGateway = object : ExtractGateway {
                override suspend fun extract(
                    rawTranscript: String,
                    notebookName: String,
                    nowIso: String,
                    hasImages: Boolean,
                ) = FastExtractResult(intent = TurnIntent.Recall, timeExplicit = true, entries = emptyList())
            },
        )

        val result = engine.process("req-recall", ProcessingMode.Background)

        assertEquals(ProcessResult.ForegroundRequired, result)
        assertEquals(0, cloudCalls)
        assertTrue(entries.calls.isEmpty())
    }

    @Test
    fun unknownRequestIsSkipped() = runBlocking {
        val store = InMemoryCaptureLedgerStore()
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = FakeTurnGateway(StructuredTurn(intent = TurnIntent.Record)),
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
        )

        val result = engine.process("missing", ProcessingMode.Background)

        assertEquals(ProcessResult.Skipped, result)
        assertTrue(entries.calls.isEmpty())
    }

    @Test
    fun emptyRecordEntriesRequireForeground() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-empty",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "嗯",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = FakeTurnGateway(
                StructuredTurn(intent = TurnIntent.Record, entries = emptyList()),
            ),
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
        )

        val result = engine.process("req-empty", ProcessingMode.Foreground)

        assertEquals(ProcessResult.ForegroundRequired, result)
        assertTrue(entries.calls.isEmpty())
        assertEquals(CaptureStatus.ForegroundRequired, store.get("req-empty")!!.status)
    }

    @Test
    fun turnFailureKeepsRetryableAndDoesNotWrite() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-fail",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "记下今天开会",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = object : TurnGateway {
                override suspend fun complete(
                    rawTranscript: String,
                    notebookId: String,
                    notebookName: String,
                    imagePaths: List<String>,
                ): StructuredTurn = error("timeout")
            },
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
        )

        val result = engine.process("req-fail", ProcessingMode.Foreground)

        assertEquals(ProcessResult.Retryable, result)
        assertTrue(entries.calls.isEmpty())
        assertEquals(CaptureStatus.Retryable, store.get("req-fail")!!.status)
        assertEquals("timeout", store.get("req-fail")!!.lastError)
    }

    @Test
    fun existingEntriesPolishOnlyWithoutRematerialize() = runBlocking {
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-polish",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "喝了杯咖啡",
                status = CaptureStatus.Retryable,
                capturedAtMillis = 1_000L,
                lastError = "润色失败",
            ),
        )
        val entries = FakeEntryMaterializer()
        var polishCalls = 0
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = FakeTurnGateway(StructuredTurn(intent = TurnIntent.Record)),
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
            polishGateway = object : PolishGateway {
                override suspend fun polish(
                    requestId: String,
                    rawTranscript: String,
                    notebookId: String,
                    notebookName: String,
                    locked: List<LockedPolishEntry>,
                    imagePaths: List<String>,
                    timeExplicit: Boolean,
                ) {
                    polishCalls++
                }
            },
            entryLocator = object : EntryLocator {
                override suspend fun listForRequest(requestId: String) = listOf(
                    LockedPolishEntry(
                        entryId = "$requestId:0",
                        startAtIso = "2026-08-22T18:00:00+08:00",
                        startAtMillis = 1L,
                    ),
                )
            },
        )

        val result = engine.process("req-polish", ProcessingMode.Background)

        assertEquals(ProcessResult.Completed, result)
        assertTrue(entries.calls.isEmpty())
        assertEquals(1, polishCalls)
    }

    @Test
    fun extractNullFallsThroughToTurnWithoutGuessing() = runBlocking {
        var turnCalls = 0
        val store = InMemoryCaptureLedgerStore(
            CaptureLedgerRecord(
                requestId = "req-402",
                notebookId = "nb-1",
                notebookName = "生活",
                rawTranscript = "上周喝咖啡那次是哪天",
                status = CaptureStatus.Pending,
                capturedAtMillis = 1_000L,
            ),
        )
        val entries = FakeEntryMaterializer()
        val engine = CaptureLedgerEngine(
            store = store,
            turnGateway = object : TurnGateway {
                override suspend fun complete(
                    rawTranscript: String,
                    notebookId: String,
                    notebookName: String,
                    imagePaths: List<String>,
                ): StructuredTurn = StructuredTurn(intent = TurnIntent.Recall).also { turnCalls++ }
            },
            materializer = entries,
            clock = { 2_000L },
            hasApiKey = { true },
            isNetworkAvailable = { true },
            extractGateway = object : ExtractGateway {
                override suspend fun extract(
                    rawTranscript: String,
                    notebookName: String,
                    nowIso: String,
                    hasImages: Boolean,
                ) = null
            },
        )
        val result = engine.process("req-402", ProcessingMode.Background)
        assertEquals(ProcessResult.ForegroundRequired, result)
        assertEquals(1, turnCalls)
        assertTrue(entries.calls.isEmpty())
    }
}

private class InMemoryCaptureLedgerStore(
    vararg rows: CaptureLedgerRecord,
) : CaptureLedgerStore {
    private val map = rows.associateBy { it.requestId }.toMutableMap()

    override suspend fun get(requestId: String): CaptureLedgerRecord? = map[requestId]

    override suspend fun upsert(record: CaptureLedgerRecord) {
        map[record.requestId] = record
    }
}

private class FakeTurnGateway(
    private val turn: StructuredTurn,
) : TurnGateway {
    override suspend fun complete(
        rawTranscript: String,
        notebookId: String,
        notebookName: String,
        imagePaths: List<String>,
    ): StructuredTurn = turn
}

private class FakeEntryMaterializer : EntryMaterializer {
    val calls = mutableListOf<MaterializePayload>()

    override suspend fun materialize(payload: MaterializePayload) {
        calls += payload
    }
}
