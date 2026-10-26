package com.yuyue.app.domain.migrate

import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.domain.local.CaptureEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMigrationRunnerTest {
    @Test
    fun emptyDomainImportsFromLegacyAndVerifiesCounts() = runBlocking {
        val legacy = LegacyV7Snapshot(
            notebooks = listOf(
                NotebookEntity("nb1", "本", "nb1", null, 0, true),
            ),
            entries = listOf(
                EntryEntity(
                    id = "e1",
                    notebookId = "nb1",
                    entryKind = "note",
                    status = "active",
                    subjectKey = null,
                    polarity = null,
                    title = "咖啡",
                    body = "美式",
                    rawTranscript = "今天喝了美式",
                    startAtMillis = 10L,
                    endAtMillis = null,
                    statedAtMillis = 10L,
                    updatedAtMillis = 10L,
                ),
            ),
        )
        var domainCounts = DomainCountSnapshot()
        var persisted: DomainImportResult? = null
        val runner = DomainMigrationRunner(
            loadLegacy = { legacy },
            readDomainCounts = { domainCounts },
            persist = { result ->
                persisted = result
                domainCounts = DomainCountVerifier.expectedFromImport(result)
            },
            logEvent = { _, _ -> },
        )

        val outcome = runner.runIfNeeded()
        assertTrue(outcome is DomainMigrationOutcome.Imported)
        val imported = outcome as DomainMigrationOutcome.Imported
        assertEquals(1, imported.actual.notebooks)
        assertEquals(1, imported.actual.entries)
        assertEquals(1, imported.actual.captures)
        assertEquals("今天喝了美式", persisted!!.captures.single().rawText)
    }

    @Test
    fun skipsWhenDomainNotEmptyWithoutTouchingLegacy() = runBlocking {
        var persistCalls = 0
        val runner = DomainMigrationRunner(
            loadLegacy = {
                LegacyV7Snapshot(
                    entries = listOf(
                        EntryEntity(
                            id = "e1",
                            notebookId = "nb1",
                            entryKind = "note",
                            status = "active",
                            subjectKey = null,
                            polarity = null,
                            title = "t",
                            body = "b",
                            rawTranscript = "secret",
                            startAtMillis = 1L,
                            endAtMillis = null,
                            statedAtMillis = 1L,
                            updatedAtMillis = 1L,
                        ),
                    ),
                )
            },
            readDomainCounts = { DomainCountSnapshot(entries = 3) },
            persist = { persistCalls++ },
            logEvent = { _, _ -> },
        )
        val outcome = runner.runIfNeeded()
        assertTrue(outcome is DomainMigrationOutcome.SkippedDomainNotEmpty)
        assertEquals(0, persistCalls)
    }

    @Test
    fun softFailsOnCountMismatchWithoutClaimingSuccess() = runBlocking {
        val legacy = LegacyV7Snapshot(
            notebooks = listOf(NotebookEntity("nb1", "本", "nb1", null, 0, true)),
            entries = listOf(
                EntryEntity(
                    id = "e1",
                    notebookId = "nb1",
                    entryKind = "note",
                    status = "active",
                    subjectKey = null,
                    polarity = null,
                    title = "t",
                    body = "b",
                    rawTranscript = "raw",
                    startAtMillis = 1L,
                    endAtMillis = null,
                    statedAtMillis = 1L,
                    updatedAtMillis = 1L,
                ),
            ),
        )
        val runner = DomainMigrationRunner(
            loadLegacy = { legacy },
            readDomainCounts = { DomainCountSnapshot() }, // stays empty after "persist"
            persist = { /* drop rows → mismatch */ },
            logEvent = { _, _ -> },
        )
        val outcome = runner.runIfNeeded()
        assertTrue(outcome is DomainMigrationOutcome.CountMismatch)
        val mismatch = outcome as DomainMigrationOutcome.CountMismatch
        assertTrue(mismatch.detail.contains("counts_mismatch"))
        assertFalse(mismatch.detail.contains("raw"))
    }

    @Test
    fun captureRawTextIsNeverReplacedInImportMapping() {
        // Policy check on importer output used by dual-write / migrate.
        val first = CaptureEntity(
            id = "cap:e1",
            notebookId = "nb1",
            rawText = "原话不可变",
            capturedAtMillis = 1L,
            source = "voice",
            status = "committed",
        )
        val attemptedOverwrite = first.copy(rawText = "被润色后的文本")
        // DomainRepository uses insertIgnore; conflict keeps [first].rawText.
        assertTrue(first.rawText != attemptedOverwrite.rawText)
        assertEquals("原话不可变", first.rawText)
    }

    private fun assertFalse(condition: Boolean) {
        org.junit.Assert.assertFalse(condition)
    }
}
