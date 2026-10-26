package com.yuyue.app.domain.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "domain_notebooks")
data class DomainNotebookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val color: String?,
    val sortOrder: Int,
    val isDefault: Boolean,
    val archivedAtMillis: Long? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

@Entity(
    tableName = "captures",
    indices = [Index("notebookId"), Index("status")],
)
data class CaptureEntity(
    @PrimaryKey val id: String,
    val notebookId: String?,
    /** Immutable original utterance; never overwritten by polish/edits. */
    val rawText: String,
    val capturedAtMillis: Long,
    val source: String,
    val status: String,
    val mediaJson: String? = null,
    val timezone: String? = null,
    val clientNowMillis: Long? = null,
    val legacyRequestId: String? = null,
)

@Entity(
    tableName = "events",
    indices = [
        Index("notebookId"),
        Index("captureId"),
        Index("startAtMillis"),
        Index("deletedAtMillis"),
    ],
)
data class EventEntity(
    @PrimaryKey val id: String,
    val notebookId: String,
    val captureId: String?,
    val kind: String,
    val status: String,
    val startAtMillis: Long,
    val endAtMillis: Long?,
    val statedAtMillis: Long,
    val updatedAtMillis: Long,
    val done: Boolean = true,
    val currentRevisionId: String?,
    val deletedAtMillis: Long? = null,
    val purgeAfterMillis: Long? = null,
    val timezone: String? = null,
    val legacyPlace: String? = null,
    val legacyPeople: String? = null,
    val legacyDiaryType: String? = null,
    val legacyEntryKind: String? = null,
    val legacySubjectKey: String? = null,
    val legacyPolarity: String? = null,
    val legacyUtteranceId: String? = null,
)

@Entity(
    tableName = "event_revisions",
    indices = [Index("eventId")],
)
data class EventRevisionEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val parentRevisionId: String? = null,
    val source: String,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "media_assets",
    indices = [Index("sha256"), Index("deletedAtMillis")],
)
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val sha256: String,
    val mime: String,
    val relativePath: String,
    val thumbRelativePath: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val createdAtMillis: Long,
    val deletedAtMillis: Long? = null,
)

@Entity(
    tableName = "event_media",
    primaryKeys = ["eventId", "assetId"],
    indices = [Index("assetId")],
)
data class EventMediaEntity(
    val eventId: String,
    val assetId: String,
    val sortOrder: Int = 0,
    val role: String = "full",
)

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val canonicalName: String,
    val aliasesJson: String = "[]",
    val createdAtMillis: Long,
)

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val canonicalName: String,
    val aliasesJson: String = "[]",
    val createdAtMillis: Long,
)

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val canonicalName: String,
    val aliasesJson: String = "[]",
    val createdAtMillis: Long,
)

@Entity(
    tableName = "event_topics",
    primaryKeys = ["eventId", "topicId"],
)
data class EventTopicEntity(
    val eventId: String,
    val topicId: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "event_people",
    primaryKeys = ["eventId", "personId"],
)
data class EventPersonEntity(
    val eventId: String,
    val personId: String,
    val role: String? = null,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "event_places",
    primaryKeys = ["eventId", "placeId"],
)
data class EventPlaceEntity(
    val eventId: String,
    val placeId: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "undo_commands",
    indices = [Index("expiresAtMillis")],
)
data class UndoCommandEntity(
    @PrimaryKey val id: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val inverseJson: String,
    val consumedAtMillis: Long? = null,
)

@Entity(
    tableName = "domain_index_jobs",
    indices = [Index("status")],
)
data class DomainIndexJobEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val jobType: String,
    val status: String,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
