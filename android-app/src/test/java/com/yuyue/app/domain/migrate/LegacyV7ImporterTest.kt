package com.yuyue.app.domain.migrate

import com.yuyue.app.data.local.CaptureLedgerEntity
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.EntryMediaEntity
import com.yuyue.app.data.local.EntryRevisionEntity
import com.yuyue.app.data.local.NotebookEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyV7ImporterTest {
    @Test
    fun emptyLegacyProducesEmptyDomain() {
        val result = LegacyV7Importer.import(LegacyV7Snapshot())
        assertTrue(result.notebooks.isEmpty())
        assertTrue(result.captures.isEmpty())
        assertTrue(result.events.isEmpty())
        assertTrue(result.revisions.isEmpty())
        assertTrue(result.mediaAssets.isEmpty())
        assertTrue(result.people.isEmpty())
        assertTrue(result.places.isEmpty())
        assertTrue(result.topics.isEmpty())
        assertTrue(result.undoCommands.isEmpty())
        assertTrue(result.indexJobs.isEmpty())
        assertNull(result.preferenceSnapshotJson)
    }

    @Test
    fun preservesEntryCountAndMapsRawTranscriptToCapture() {
        val snapshot = LegacyV7Snapshot(
            notebooks = listOf(nb("nb1")),
            entries = listOf(
                entry(
                    id = "e1",
                    raw = "今天喝了美式",
                    title = "咖啡",
                    body = "美式一杯",
                    place = "咖啡馆",
                    people = "小王,小李",
                    diaryType = "food",
                ),
                entry(id = "e2", raw = "去公园", title = "散步", body = "傍晚散步"),
            ),
        )
        val result = LegacyV7Importer.import(snapshot)
        assertEquals(1, result.notebooks.size)
        assertEquals(2, result.events.size)
        assertEquals(2, result.captures.size)
        assertEquals("今天喝了美式", result.captures.single { it.id == "cap:e1" }.rawText)
        assertEquals("去公园", result.captures.single { it.id == "cap:e2" }.rawText)
        val e1 = result.events.single { it.id == "e1" }
        assertEquals("cap:e1", e1.captureId)
        assertNotNull(e1.currentRevisionId)
        assertEquals("咖啡馆", e1.legacyPlace)
        assertEquals("小王,小李", e1.legacyPeople)
        assertEquals("food", e1.legacyDiaryType)
        val current = result.revisions.single { it.id == e1.currentRevisionId }
        assertEquals("咖啡", current.title)
        assertEquals("美式一杯", current.body)
        assertTrue(result.captures.none { it.rawText == "美式一杯" })
    }

    @Test
    fun mediaStoresRelativePathShaAndSoftDelete() {
        val snapshot = LegacyV7Snapshot(
            notebooks = listOf(nb("nb1")),
            entries = listOf(
                entry(id = "e1", raw = "有图", title = "图", body = "图", deletedAt = 1_700_000_000_000L),
            ),
            media = listOf(
                EntryMediaEntity(
                    id = "m1",
                    entryId = "e1",
                    requestId = "r1",
                    mime = "image/jpeg",
                    localPath = "/data/user/0/com.yuyue.app/files/entry_media/m1.jpg",
                    thumbPath = "/data/user/0/com.yuyue.app/files/entry_media/m1_thumb.jpg",
                    width = 100,
                    height = 80,
                    sortOrder = 0,
                    createdAtMillis = 1_600_000_000_000L,
                ),
            ),
            mediaSha256ById = mapOf("m1" to "abc123sha"),
        )
        val result = LegacyV7Importer.import(snapshot)
        assertEquals(1, result.mediaAssets.size)
        val asset = result.mediaAssets.single()
        assertEquals("media/m1.jpg", asset.relativePath)
        assertEquals("abc123sha", asset.sha256)
        assertNotNull(asset.deletedAtMillis)
        assertEquals(1, result.eventMedia.size)
        assertEquals("e1", result.eventMedia.single().eventId)
        assertNotNull(result.events.single().deletedAtMillis)
    }

    @Test
    fun incompleteLedgerBecomesInboxCaptures() {
        val snapshot = LegacyV7Snapshot(
            notebooks = listOf(nb("nb1")),
            incompleteLedger = listOf(
                CaptureLedgerEntity(
                    requestId = "req1",
                    utteranceId = "utt1",
                    notebookId = "nb1",
                    notebookName = "生活",
                    rawTranscript = "还没处理完",
                    capturedAtMillis = 1_650_000_000_000L,
                    status = "Pending",
                ),
            ),
        )
        val result = LegacyV7Importer.import(snapshot)
        assertEquals(1, result.captures.size)
        val cap = result.captures.single()
        assertEquals("还没处理完", cap.rawText)
        assertEquals("inbox", cap.status)
        assertEquals("voice", cap.source)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun preferenceSnapshotPreserved() {
        val snapshot = LegacyV7Snapshot(
            preferenceSnapshot = mapOf(
                "theme" to "paper",
                "displayName" to "江",
            ),
        )
        val result = LegacyV7Importer.import(snapshot)
        assertNotNull(result.preferenceSnapshotJson)
        assertTrue(result.preferenceSnapshotJson!!.contains("paper"))
        assertTrue(result.preferenceSnapshotJson!!.contains("江"))
    }

    @Test
    fun corruptMissingFieldsHandledSafely() {
        val snapshot = LegacyV7Snapshot(
            notebooks = listOf(nb("nb1")),
            entries = listOf(
                entry(id = "e-blank", raw = null, title = "", body = "", people = null, place = null),
            ),
            revisions = listOf(
                EntryRevisionEntity(
                    id = "bad",
                    entryId = "missing-entry",
                    snapshotJson = "{not-json",
                    createdAtMillis = 1L,
                    reason = "edit",
                ),
                EntryRevisionEntity(
                    id = "ok",
                    entryId = "e-blank",
                    snapshotJson =
                        """{"id":"e-blank","notebookId":"nb1","entryKind":"note","status":"active",""" +
                            """"title":"旧标题","body":"旧正文","startAtMillis":1,"statedAtMillis":1,"updatedAtMillis":1}""",
                    createdAtMillis = 2L,
                    reason = "edit",
                ),
            ),
            media = listOf(
                EntryMediaEntity(
                    id = "orphan",
                    entryId = "gone",
                    requestId = "",
                    mime = "image/jpeg",
                    localPath = "",
                    thumbPath = "",
                    width = 0,
                    height = 0,
                    sortOrder = 0,
                    createdAtMillis = 0L,
                ),
            ),
        )
        val result = LegacyV7Importer.import(snapshot)
        assertEquals(1, result.events.size)
        assertEquals(1, result.captures.size)
        val cap = result.captures.single()
        assertEquals("", cap.rawText)
        assertTrue(result.revisions.any { it.title == "旧标题" })
        assertTrue(result.revisions.none { it.id == "bad" })
        assertTrue(result.mediaAssets.isEmpty())
    }

    @Test
    fun appendOnlyRevisionsNeverOverwriteCapture() {
        val snapshot = LegacyV7Snapshot(
            notebooks = listOf(nb("nb1")),
            entries = listOf(
                entry(id = "e1", raw = "原话不动", title = "润色标题", body = "润色正文"),
            ),
            revisions = listOf(
                EntryRevisionEntity(
                    id = "r1",
                    entryId = "e1",
                    snapshotJson =
                        """{"id":"e1","notebookId":"nb1","entryKind":"note","status":"active",""" +
                            """"title":"更早","body":"更早正文","rawTranscript":"原话不动",""" +
                            """"startAtMillis":10,"statedAtMillis":10,"updatedAtMillis":10}""",
                    createdAtMillis = 10L,
                    reason = "edit",
                ),
            ),
        )
        val result = LegacyV7Importer.import(snapshot)
        assertEquals("原话不动", result.captures.single().rawText)
        assertTrue(result.revisions.size >= 2)
        assertTrue(result.revisions.all { it.eventId == "e1" })
        val originals = result.revisions.filter { it.source == "original" }
        assertEquals(1, originals.size)
        assertTrue(originals.single().body.contains("原话不动") || originals.single().title.isNotBlank())
    }

    private fun nb(id: String) =
        NotebookEntity(id, "生活", "life", "#111", 0, true)

    private fun entry(
        id: String,
        raw: String?,
        title: String,
        body: String,
        place: String? = null,
        people: String? = null,
        diaryType: String? = null,
        deletedAt: Long? = null,
    ) = EntryEntity(
        id = id,
        notebookId = "nb1",
        entryKind = "note",
        status = "active",
        subjectKey = null,
        polarity = null,
        title = title,
        body = body,
        rawTranscript = raw,
        startAtMillis = 1_600_000_000_000L,
        endAtMillis = null,
        statedAtMillis = 1_600_000_000_000L,
        updatedAtMillis = 1_600_000_100_000L,
        deletedAtMillis = deletedAt,
        done = true,
        utteranceId = null,
        place = place,
        people = people,
        diaryType = diaryType,
    )
}
