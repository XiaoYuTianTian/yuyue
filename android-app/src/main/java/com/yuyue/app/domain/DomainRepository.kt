package com.yuyue.app.domain

import com.yuyue.app.data.capture.CaptureCommand
import com.yuyue.app.data.capture.MaterializePayload
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.domain.local.CaptureEntity
import com.yuyue.app.domain.local.DomainNotebookEntity
import com.yuyue.app.domain.local.EventEntity
import com.yuyue.app.domain.local.EventRevisionEntity
import com.yuyue.app.domain.local.YuyueDomainDatabase
import com.yuyue.app.domain.migrate.DomainCountSnapshot
import com.yuyue.app.domain.migrate.DomainImportResult
import com.yuyue.app.observability.PrivacyLog
import com.yuyue.app.util.TimeUtils
import java.time.ZoneId

/**
 * Domain-side writes for cutover / dual-write.
 * Capture.rawText is insert-once; revisions are append-only (IGNORE).
 */
class DomainRepository(
    private val db: YuyueDomainDatabase,
    private val writeEnabled: Boolean = true,
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai"),
) {
    suspend fun counts(): DomainCountSnapshot = DomainCountSnapshot(
        notebooks = db.notebookDao().count(),
        entries = db.eventDao().count(),
        captures = db.captureDao().count(),
        media = db.mediaAssetDao().count(),
    )

    suspend fun isEmpty(): Boolean = counts().isEmpty()

    /**
     * Bulk persist importer output. Safe on empty domain (REPLACE upserts).
     * Revisions use IGNORE so re-runs do not clobber history.
     */
    suspend fun persistImport(result: DomainImportResult) {
        if (result.notebooks.isNotEmpty()) db.notebookDao().upsertAll(result.notebooks)
        if (result.captures.isNotEmpty()) db.captureDao().upsertAll(result.captures)
        if (result.events.isNotEmpty()) db.eventDao().upsertAll(result.events)
        if (result.revisions.isNotEmpty()) db.eventRevisionDao().insertAll(result.revisions)
        if (result.mediaAssets.isNotEmpty()) db.mediaAssetDao().upsertAll(result.mediaAssets)
        if (result.eventMedia.isNotEmpty()) db.eventMediaDao().upsertAll(result.eventMedia)
        if (result.topics.isNotEmpty()) db.topicDao().upsertAll(result.topics)
        if (result.people.isNotEmpty()) db.personDao().upsertAll(result.people)
        if (result.places.isNotEmpty()) db.placeDao().upsertAll(result.places)
        if (result.eventTopics.isNotEmpty()) db.eventTopicDao().upsertAll(result.eventTopics)
        if (result.eventPeople.isNotEmpty()) db.eventPersonDao().upsertAll(result.eventPeople)
        if (result.eventPlaces.isNotEmpty()) db.eventPlaceDao().upsertAll(result.eventPlaces)
        if (result.undoCommands.isNotEmpty()) db.undoCommandDao().upsertAll(result.undoCommands)
        if (result.indexJobs.isNotEmpty()) db.indexJobDao().upsertAll(result.indexJobs)
    }

    /** Insert-only: existing Capture.rawText is never overwritten. */
    suspend fun insertCaptureIfAbsent(capture: CaptureEntity): Boolean {
        if (!writeEnabled) return false
        val existing = db.captureDao().getById(capture.id)
        if (existing != null) return false
        return db.captureDao().insertIgnore(capture) != -1L
    }

    /**
     * Update capture status/notebook/media without touching rawText.
     * No-op if the row does not exist.
     */
    suspend fun updateCaptureMeta(
        id: String,
        status: String,
        notebookId: String? = null,
        mediaJson: String? = null,
    ) {
        if (!writeEnabled) return
        if (db.captureDao().getById(id) == null) return
        db.captureDao().updateMeta(id, status, notebookId, mediaJson)
    }

    /** Append-only revision; conflict ignored. */
    suspend fun appendRevision(revision: EventRevisionEntity): Boolean {
        if (!writeEnabled) return false
        db.eventRevisionDao().insertAll(listOf(revision))
        return true
    }

    suspend fun upsertEvent(event: EventEntity) {
        if (!writeEnabled) return
        db.eventDao().upsertAll(listOf(event))
    }

    suspend fun upsertNotebook(notebook: DomainNotebookEntity) {
        if (!writeEnabled) return
        db.notebookDao().upsertAll(listOf(notebook))
    }

    /** Inbox dual-write from CaptureRawStore.saveRaw. */
    suspend fun mirrorInboxCapture(command: CaptureCommand, requestId: String) {
        if (!writeEnabled) return
        runCatching {
            val mediaJson = command.mediaPaths.takeIf { it.isNotEmpty() }?.let { paths ->
                paths.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            }
            insertCaptureIfAbsent(
                CaptureEntity(
                    id = "cap:ledger:$requestId",
                    notebookId = command.notebookId.ifBlank { null },
                    rawText = command.text,
                    capturedAtMillis = command.capturedAtMillis,
                    source = when (command.source.name.lowercase()) {
                        "voice" -> "voice"
                        "photo", "share" -> "photo"
                        else -> "type"
                    },
                    status = "inbox",
                    mediaJson = mediaJson,
                    timezone = command.timezone.id,
                    clientNowMillis = command.capturedAtMillis,
                    legacyRequestId = requestId,
                ),
            )
        }.onFailure {
            runCatching {
                PrivacyLog.event("domain_dual_write_fail", mapOf("stage" to "inbox"))
            }
        }
    }

    /**
     * Dual-write materialized entries after legacy [YuyueRepository.saveStructuredEntries].
     * [entryIds] must align with [payload.entries] order.
     */
    suspend fun mirrorMaterialized(payload: MaterializePayload, entryIds: List<String>) {
        if (!writeEnabled) return
        runCatching {
            val sharedRaw = payload.entries.firstOrNull()?.rawTranscript.orEmpty()
            val now = System.currentTimeMillis()
            for ((idx, entryId) in entryIds.withIndex()) {
                val item = payload.entries.getOrNull(idx) ?: continue
                val raw = item.rawTranscript.ifBlank { sharedRaw }
                val captureId = "cap:$entryId"
                insertCaptureIfAbsent(
                    CaptureEntity(
                        id = captureId,
                        notebookId = payload.notebookId,
                        rawText = raw,
                        capturedAtMillis = now,
                        source = "voice",
                        status = "committed",
                        legacyRequestId = payload.requestId,
                    ),
                )
                val originalRevId = "rev:$entryId:original"
                appendRevision(
                    EventRevisionEntity(
                        id = originalRevId,
                        eventId = entryId,
                        parentRevisionId = null,
                        source = "original",
                        title = if (raw.isNotBlank()) raw.take(40) else item.title.ifBlank { "原话" },
                        body = raw,
                        createdAtMillis = now,
                    ),
                )
                val currentRevId = "rev:$entryId:current"
                appendRevision(
                    EventRevisionEntity(
                        id = currentRevId,
                        eventId = entryId,
                        parentRevisionId = originalRevId,
                        source = "import",
                        title = item.title,
                        body = item.body,
                        createdAtMillis = now,
                    ),
                )
                val startMs = runCatching {
                    TimeUtils.parseIso(item.startAt).toEpochMilli()
                }.getOrDefault(now)
                upsertEvent(
                    EventEntity(
                        id = entryId,
                        notebookId = payload.notebookId,
                        captureId = captureId,
                        kind = if (item.diaryType.equals("task", ignoreCase = true)) "task" else "note",
                        status = "active",
                        startAtMillis = startMs,
                        endAtMillis = null,
                        statedAtMillis = now,
                        updatedAtMillis = now,
                        done = true,
                        currentRevisionId = currentRevId,
                        legacyPlace = item.place.ifBlank { null },
                        legacyPeople = item.people.filter { it.isNotBlank() }.joinToString("、").ifBlank { null },
                        legacyDiaryType = item.diaryType.ifBlank { null },
                        legacyEntryKind = "note",
                        legacyUtteranceId = payload.utteranceId,
                    ),
                )
            }
            updateCaptureMeta(
                id = "cap:ledger:${payload.requestId}",
                status = "committed",
                notebookId = payload.notebookId,
            )
        }.onFailure {
            runCatching {
                PrivacyLog.event(
                    "domain_dual_write_fail",
                    mapOf("stage" to "materialize", "counts" to entryIds.size.toString()),
                )
            }
        }
    }

    /** Mirror a single legacy entry row (for adapters / tests). */
    suspend fun mirrorLegacyEntry(entry: EntryEntity) {
        if (!writeEnabled) return
        val captureId = "cap:${entry.id}"
        val raw = entry.rawTranscript.orEmpty()
        insertCaptureIfAbsent(
            CaptureEntity(
                id = captureId,
                notebookId = entry.notebookId,
                rawText = raw,
                capturedAtMillis = entry.statedAtMillis,
                source = if (!entry.utteranceId.isNullOrBlank()) "voice" else "type",
                status = "committed",
                legacyRequestId = entry.utteranceId,
            ),
        )
        val originalRevId = "rev:${entry.id}:original"
        appendRevision(
            EventRevisionEntity(
                id = originalRevId,
                eventId = entry.id,
                parentRevisionId = null,
                source = "original",
                title = if (raw.isNotBlank()) raw.take(40) else entry.title.ifBlank { "原话" },
                body = raw,
                createdAtMillis = entry.statedAtMillis,
            ),
        )
        val currentRevId = "rev:${entry.id}:current"
        appendRevision(
            EventRevisionEntity(
                id = currentRevId,
                eventId = entry.id,
                parentRevisionId = originalRevId,
                source = "import",
                title = entry.title,
                body = entry.body,
                createdAtMillis = entry.updatedAtMillis,
            ),
        )
        upsertEvent(
            EventEntity(
                id = entry.id,
                notebookId = entry.notebookId,
                captureId = captureId,
                kind = entry.entryKind.ifBlank { "note" },
                status = entry.status.ifBlank { "active" },
                startAtMillis = entry.startAtMillis,
                endAtMillis = entry.endAtMillis,
                statedAtMillis = entry.statedAtMillis,
                updatedAtMillis = entry.updatedAtMillis,
                done = entry.done,
                currentRevisionId = currentRevId,
                deletedAtMillis = entry.deletedAtMillis,
                legacyPlace = entry.place,
                legacyPeople = entry.people,
                legacyDiaryType = entry.diaryType,
                legacyEntryKind = entry.entryKind,
                legacySubjectKey = entry.subjectKey,
                legacyPolarity = entry.polarity,
                legacyUtteranceId = entry.utteranceId,
            ),
        )
    }
}
