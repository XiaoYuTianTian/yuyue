package com.yuyue.app.domain.migrate

import com.yuyue.app.data.EntrySnapshot
import com.yuyue.app.data.local.CaptureLedgerEntity
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.EntryMediaEntity
import com.yuyue.app.data.local.EntryRevisionEntity
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.domain.local.CaptureEntity
import com.yuyue.app.domain.local.DomainIndexJobEntity
import com.yuyue.app.domain.local.DomainNotebookEntity
import com.yuyue.app.domain.local.EventEntity
import com.yuyue.app.domain.local.EventMediaEntity
import com.yuyue.app.domain.local.EventPersonEntity
import com.yuyue.app.domain.local.EventPlaceEntity
import com.yuyue.app.domain.local.EventRevisionEntity
import com.yuyue.app.domain.local.EventTopicEntity
import com.yuyue.app.domain.local.MediaAssetEntity
import com.yuyue.app.domain.local.PersonEntity
import com.yuyue.app.domain.local.PlaceEntity
import com.yuyue.app.domain.local.TopicEntity
import com.yuyue.app.domain.local.UndoCommandEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data class LegacyV7Snapshot(
    val notebooks: List<NotebookEntity> = emptyList(),
    val entries: List<EntryEntity> = emptyList(),
    val revisions: List<EntryRevisionEntity> = emptyList(),
    val media: List<EntryMediaEntity> = emptyList(),
    val incompleteLedger: List<CaptureLedgerEntity> = emptyList(),
    val preferenceSnapshot: Map<String, String> = emptyMap(),
    /** Optional precomputed content hashes; missing ids get empty sha and are still imported when path is valid. */
    val mediaSha256ById: Map<String, String> = emptyMap(),
)

data class DomainImportResult(
    val notebooks: List<DomainNotebookEntity> = emptyList(),
    val captures: List<CaptureEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val revisions: List<EventRevisionEntity> = emptyList(),
    val mediaAssets: List<MediaAssetEntity> = emptyList(),
    val eventMedia: List<EventMediaEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val places: List<PlaceEntity> = emptyList(),
    val eventTopics: List<EventTopicEntity> = emptyList(),
    val eventPeople: List<EventPersonEntity> = emptyList(),
    val eventPlaces: List<EventPlaceEntity> = emptyList(),
    val undoCommands: List<UndoCommandEntity> = emptyList(),
    val indexJobs: List<DomainIndexJobEntity> = emptyList(),
    val preferenceSnapshotJson: String? = null,
)

/**
 * Pure Kotlin mapper from legacy Room v7 rows into the domain model.
 * Does not open databases or touch personal package data.
 */
object LegacyV7Importer {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; isLenient = true }

    private val completedLedgerStatuses = setOf("Completed")

    fun import(snapshot: LegacyV7Snapshot): DomainImportResult {
        val notebooks = snapshot.notebooks.map { nb ->
            DomainNotebookEntity(
                id = nb.id,
                name = nb.name,
                slug = nb.slug,
                color = nb.color,
                sortOrder = nb.sortOrder,
                isDefault = nb.isDefault,
                createdAtMillis = 0L,
                updatedAtMillis = 0L,
            )
        }

        val captures = mutableListOf<CaptureEntity>()
        val events = mutableListOf<EventEntity>()
        val revisions = mutableListOf<EventRevisionEntity>()
        val topics = linkedMapOf<String, TopicEntity>()
        val people = linkedMapOf<String, PersonEntity>()
        val places = linkedMapOf<String, PlaceEntity>()
        val eventTopics = mutableListOf<EventTopicEntity>()
        val eventPeople = mutableListOf<EventPersonEntity>()
        val eventPlaces = mutableListOf<EventPlaceEntity>()
        val indexJobs = mutableListOf<DomainIndexJobEntity>()

        val entryIds = snapshot.entries.map { it.id }.toSet()
        val revisionsByEntry = snapshot.revisions
            .filter { it.entryId in entryIds }
            .groupBy { it.entryId }

        for (entry in snapshot.entries) {
            val captureId = "cap:${entry.id}"
            val rawText = entry.rawTranscript ?: ""
            captures += CaptureEntity(
                id = captureId,
                notebookId = entry.notebookId,
                rawText = rawText,
                capturedAtMillis = entry.statedAtMillis,
                source = inferSource(entry),
                status = "committed",
                legacyRequestId = entry.utteranceId,
            )

            val originalRevId = "rev:${entry.id}:original"
            revisions += EventRevisionEntity(
                id = originalRevId,
                eventId = entry.id,
                parentRevisionId = null,
                source = "original",
                title = if (rawText.isNotBlank()) rawText.take(40) else entry.title.ifBlank { "原话" },
                body = rawText,
                createdAtMillis = entry.statedAtMillis,
            )

            var parentId: String? = originalRevId
            val historical = revisionsByEntry[entry.id].orEmpty().sortedBy { it.createdAtMillis }
            for (legacyRev in historical) {
                val snap = decodeSnapshotOrNull(legacyRev.snapshotJson) ?: continue
                val revId = "rev:legacy:${legacyRev.id}"
                revisions += EventRevisionEntity(
                    id = revId,
                    eventId = entry.id,
                    parentRevisionId = parentId,
                    source = mapRevisionReason(legacyRev.reason),
                    title = snap.title,
                    body = snap.body,
                    createdAtMillis = legacyRev.createdAtMillis,
                )
                parentId = revId
            }

            val currentRevId = "rev:${entry.id}:current"
            revisions += EventRevisionEntity(
                id = currentRevId,
                eventId = entry.id,
                parentRevisionId = parentId,
                source = "import",
                title = entry.title,
                body = entry.body,
                createdAtMillis = entry.updatedAtMillis,
            )

            events += EventEntity(
                id = entry.id,
                notebookId = entry.notebookId,
                captureId = captureId,
                kind = mapKind(entry),
                status = entry.status.ifBlank { "active" },
                startAtMillis = entry.startAtMillis,
                endAtMillis = entry.endAtMillis,
                statedAtMillis = entry.statedAtMillis,
                updatedAtMillis = entry.updatedAtMillis,
                done = entry.done,
                currentRevisionId = currentRevId,
                deletedAtMillis = entry.deletedAtMillis,
                purgeAfterMillis = entry.deletedAtMillis?.let { it + 30L * 24 * 60 * 60 * 1000 },
                legacyPlace = entry.place,
                legacyPeople = entry.people,
                legacyDiaryType = entry.diaryType,
                legacyEntryKind = entry.entryKind,
                legacySubjectKey = entry.subjectKey,
                legacyPolarity = entry.polarity,
                legacyUtteranceId = entry.utteranceId,
            )

            linkEntities(
                entry = entry,
                topics = topics,
                people = people,
                places = places,
                eventTopics = eventTopics,
                eventPeople = eventPeople,
                eventPlaces = eventPlaces,
            )

            indexJobs += DomainIndexJobEntity(
                id = "idx:${entry.id}",
                eventId = entry.id,
                jobType = "embed_fts",
                status = "pending",
                createdAtMillis = entry.updatedAtMillis,
                updatedAtMillis = entry.updatedAtMillis,
            )
        }

        for (ledger in snapshot.incompleteLedger) {
            if (ledger.status in completedLedgerStatuses) continue
            if (ledger.rawTranscript.isBlank() && ledger.mediaJson.isNullOrBlank()) continue
            captures += CaptureEntity(
                id = "cap:ledger:${ledger.requestId}",
                notebookId = ledger.notebookId.ifBlank { null },
                rawText = ledger.rawTranscript,
                capturedAtMillis = ledger.capturedAtMillis,
                source = "voice",
                status = "inbox",
                mediaJson = ledger.mediaJson,
                legacyRequestId = ledger.requestId,
            )
        }

        val mediaAssets = mutableListOf<MediaAssetEntity>()
        val eventMedia = mutableListOf<EventMediaEntity>()
        val deletedByEntry = snapshot.entries.associate { it.id to it.deletedAtMillis }
        for (m in snapshot.media) {
            if (m.entryId !in entryIds) continue
            val relative = toRelativeMediaPath(m.localPath, m.id) ?: continue
            val thumbRel = toRelativeMediaPath(m.thumbPath, "${m.id}_thumb")
            val deletedAt = deletedByEntry[m.entryId]
            mediaAssets += MediaAssetEntity(
                id = m.id,
                sha256 = snapshot.mediaSha256ById[m.id].orEmpty().ifBlank { "unknown" },
                mime = m.mime.ifBlank { "image/jpeg" },
                relativePath = relative,
                thumbRelativePath = thumbRel,
                width = m.width,
                height = m.height,
                createdAtMillis = m.createdAtMillis,
                deletedAtMillis = deletedAt,
            )
            eventMedia += EventMediaEntity(
                eventId = m.entryId,
                assetId = m.id,
                sortOrder = m.sortOrder,
                role = "full",
            )
        }

        val prefsJson = if (snapshot.preferenceSnapshot.isEmpty()) {
            null
        } else {
            runCatching { json.encodeToString(snapshot.preferenceSnapshot) }.getOrNull()
        }

        return DomainImportResult(
            notebooks = notebooks,
            captures = captures,
            events = events,
            revisions = revisions,
            mediaAssets = mediaAssets,
            eventMedia = eventMedia,
            topics = topics.values.toList(),
            people = people.values.toList(),
            places = places.values.toList(),
            eventTopics = eventTopics,
            eventPeople = eventPeople,
            eventPlaces = eventPlaces,
            undoCommands = emptyList(),
            indexJobs = indexJobs,
            preferenceSnapshotJson = prefsJson,
        )
    }

    private fun inferSource(entry: EntryEntity): String =
        when {
            !entry.utteranceId.isNullOrBlank() -> "voice"
            else -> "type"
        }

    private fun mapKind(entry: EntryEntity): String {
        val diary = entry.diaryType?.lowercase().orEmpty()
        return when {
            diary == "task" || diary == "plan" -> "task"
            entry.entryKind.equals("task", ignoreCase = true) -> "task"
            diary.isNotBlank() -> "note"
            else -> entry.entryKind.ifBlank { "note" }
        }
    }

    private fun mapRevisionReason(reason: String): String =
        when (reason.lowercase()) {
            "capture" -> "extract"
            "edit" -> "user"
            "restore" -> "restore"
            "import" -> "import"
            "delete", "set_done" -> "user"
            else -> "import"
        }

    private fun decodeSnapshotOrNull(raw: String): EntrySnapshot? =
        runCatching { json.decodeFromString(EntrySnapshot.serializer(), raw) }.getOrNull()

    private fun toRelativeMediaPath(absoluteOrRelative: String, fallbackId: String): String? {
        val trimmed = absoluteOrRelative.trim()
        if (trimmed.isEmpty()) return null
        val name = File(trimmed).name
        if (name.isBlank() || name == "." || name == "..") return null
        return "media/$name".takeIf { name.contains('.') } ?: "media/$fallbackId.jpg"
    }

    private fun linkEntities(
        entry: EntryEntity,
        topics: MutableMap<String, TopicEntity>,
        people: MutableMap<String, PersonEntity>,
        places: MutableMap<String, PlaceEntity>,
        eventTopics: MutableList<EventTopicEntity>,
        eventPeople: MutableList<EventPersonEntity>,
        eventPlaces: MutableList<EventPlaceEntity>,
    ) {
        val at = entry.updatedAtMillis
        entry.diaryType?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            val id = "topic:${name.lowercase()}"
            topics.putIfAbsent(id, TopicEntity(id, name, createdAtMillis = at))
            eventTopics += EventTopicEntity(entry.id, id)
        }
        // Keep legacy strings on Event; only split on clear separators to avoid fidelity loss.
        splitPeople(entry.people).forEachIndexed { idx, name ->
            val id = "person:${name.lowercase()}"
            people.putIfAbsent(id, PersonEntity(id, name, createdAtMillis = at))
            eventPeople += EventPersonEntity(entry.id, id, sortOrder = idx)
        }
        entry.place?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            val id = "place:${name.lowercase()}"
            places.putIfAbsent(id, PlaceEntity(id, name, createdAtMillis = at))
            eventPlaces += EventPlaceEntity(entry.id, id)
        }
    }

    private fun splitPeople(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val parts = raw.split(',', '，', ';', '；', '、')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        // If split would invent false boundaries (no separator), keep as single entity.
        return if (parts.size <= 1) listOf(raw.trim()) else parts
    }
}
