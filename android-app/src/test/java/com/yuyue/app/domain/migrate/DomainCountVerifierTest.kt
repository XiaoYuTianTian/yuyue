package com.yuyue.app.domain.migrate

import com.yuyue.app.data.local.CaptureLedgerEntity
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.EntryMediaEntity
import com.yuyue.app.data.local.NotebookEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainCountVerifierTest {
    @Test
    fun verifyPassesWhenAllCountsMatch() {
        val snap = DomainCountSnapshot(notebooks = 1, entries = 2, captures = 3, media = 4)
        val result = DomainCountVerifier.verify(snap, snap)
        assertTrue(result.ok)
        assertTrue(result.message.startsWith("counts_ok"))
    }

    @Test
    fun verifyFailsWithPrivacySafeDetail() {
        val expected = DomainCountSnapshot(notebooks = 1, entries = 2, captures = 2, media = 1)
        val actual = DomainCountSnapshot(notebooks = 1, entries = 1, captures = 2, media = 1)
        val result = DomainCountVerifier.verify(expected, actual)
        assertFalse(result.ok)
        assertTrue(result.message.contains("entries expected=2 actual=1"))
        assertFalse(result.message.contains("今天"))
        assertFalse(result.message.contains("transcript"))
    }

    @Test
    fun legacySourceCountsConserveNotebooksEntriesMediaAndDeriveCaptures() {
        val snapshot = LegacyV7Snapshot(
            notebooks = listOf(nb("nb1")),
            entries = listOf(
                entry("e1", "今天喝了美式"),
                entry("e2", "去公园"),
            ),
            media = listOf(
                media("m1", "e1"),
                media("m2", "orphan"), // skipped by importer
            ),
            incompleteLedger = listOf(
                ledger("r1", "还没处理完", "Pending"),
                ledger("r2", "", "Completed"),
            ),
        )
        val counts = DomainCountVerifier.legacySourceCounts(snapshot)
        assertEquals(1, counts.notebooks)
        assertEquals(2, counts.entries)
        assertEquals(3, counts.captures) // 2 entries + 1 incomplete
        assertEquals(1, counts.media)

        val imported = LegacyV7Importer.import(snapshot)
        val fromImport = DomainCountVerifier.expectedFromImport(imported)
        assertEquals(counts.notebooks, fromImport.notebooks)
        assertEquals(counts.entries, fromImport.entries)
        assertEquals(counts.captures, fromImport.captures)
        assertEquals(counts.media, fromImport.media)
    }

    @Test
    fun emptyLegacyProducesEmptyExpectedCounts() {
        val counts = DomainCountVerifier.legacySourceCounts(LegacyV7Snapshot())
        assertTrue(counts.isEmpty())
        val fromImport = DomainCountVerifier.expectedFromImport(LegacyV7Importer.import(LegacyV7Snapshot()))
        assertTrue(fromImport.isEmpty())
    }

    private fun nb(id: String) = NotebookEntity(
        id = id,
        name = "本",
        slug = id,
        color = null,
        sortOrder = 0,
        isDefault = true,
    )

    private fun entry(id: String, raw: String) = EntryEntity(
        id = id,
        notebookId = "nb1",
        entryKind = "note",
        status = "active",
        subjectKey = null,
        polarity = null,
        title = "t",
        body = "b",
        rawTranscript = raw,
        startAtMillis = 1L,
        endAtMillis = null,
        statedAtMillis = 1L,
        updatedAtMillis = 1L,
    )

    private fun media(id: String, entryId: String) = EntryMediaEntity(
        id = id,
        entryId = entryId,
        requestId = "req",
        mime = "image/jpeg",
        localPath = "/x/$id.jpg",
        thumbPath = "/x/$id.jpg",
        width = 1,
        height = 1,
        sortOrder = 0,
        createdAtMillis = 1L,
    )

    private fun ledger(id: String, raw: String, status: String) = CaptureLedgerEntity(
        requestId = id,
        utteranceId = id,
        notebookId = "nb1",
        notebookName = "本",
        rawTranscript = raw,
        capturedAtMillis = 1L,
        status = status,
    )
}
