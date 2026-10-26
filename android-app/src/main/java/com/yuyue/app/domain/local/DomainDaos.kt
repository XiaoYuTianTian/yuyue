package com.yuyue.app.domain.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DomainNotebookDao {
    @Query("SELECT * FROM domain_notebooks ORDER BY sortOrder")
    suspend fun listAll(): List<DomainNotebookEntity>

    @Query("SELECT COUNT(*) FROM domain_notebooks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DomainNotebookEntity>)
}

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CaptureEntity?

    @Query("SELECT COUNT(*) FROM captures")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CaptureEntity>)

    /** Never overwrites existing rawText; use for ongoing dual-write. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: CaptureEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(items: List<CaptureEntity>)

    @Query(
        """
        UPDATE captures SET status = :status, notebookId = :notebookId, mediaJson = :mediaJson
        WHERE id = :id
        """,
    )
    suspend fun updateMeta(id: String, status: String, notebookId: String?, mediaJson: String?)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventEntity?

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EventEntity>)

    @Query("UPDATE events SET currentRevisionId = :revisionId, updatedAtMillis = :updatedAt WHERE id = :eventId")
    suspend fun setCurrentRevision(eventId: String, revisionId: String, updatedAt: Long)
}

@Dao
interface EventRevisionDao {
    @Query("SELECT * FROM event_revisions WHERE eventId = :eventId ORDER BY createdAtMillis ASC")
    suspend fun listForEvent(eventId: String): List<EventRevisionEntity>

    @Query("SELECT COUNT(*) FROM event_revisions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<EventRevisionEntity>)
}

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MediaAssetEntity?

    @Query("SELECT COUNT(*) FROM media_assets WHERE deletedAtMillis IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM media_assets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaAssetEntity>)
}

@Dao
interface EventMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EventMediaEntity>)

    @Query("SELECT COUNT(*) FROM event_media WHERE assetId = :assetId")
    suspend fun refCount(assetId: String): Int
}

@Dao
interface TopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TopicEntity>)
}

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PersonEntity>)
}

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PlaceEntity>)
}

@Dao
interface EventTopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EventTopicEntity>)
}

@Dao
interface EventPersonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EventPersonEntity>)
}

@Dao
interface EventPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EventPlaceEntity>)
}

@Dao
interface UndoCommandDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<UndoCommandEntity>)

    @Query("SELECT * FROM undo_commands WHERE consumedAtMillis IS NULL AND expiresAtMillis > :now ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun latestOpen(now: Long): UndoCommandEntity?
}

@Dao
interface DomainIndexJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DomainIndexJobEntity>)

    @Query("SELECT * FROM domain_index_jobs WHERE status = :status ORDER BY createdAtMillis ASC LIMIT :limit")
    suspend fun listByStatus(status: String, limit: Int = 50): List<DomainIndexJobEntity>
}
