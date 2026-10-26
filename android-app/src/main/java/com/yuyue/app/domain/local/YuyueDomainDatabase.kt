package com.yuyue.app.domain.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DomainNotebookEntity::class,
        CaptureEntity::class,
        EventEntity::class,
        EventRevisionEntity::class,
        MediaAssetEntity::class,
        EventMediaEntity::class,
        TopicEntity::class,
        PersonEntity::class,
        PlaceEntity::class,
        EventTopicEntity::class,
        EventPersonEntity::class,
        EventPlaceEntity::class,
        UndoCommandEntity::class,
        DomainIndexJobEntity::class,
    ],
    version = YuyueDomainDatabase.VERSION,
    exportSchema = true,
)
abstract class YuyueDomainDatabase : RoomDatabase() {
    abstract fun notebookDao(): DomainNotebookDao
    abstract fun captureDao(): CaptureDao
    abstract fun eventDao(): EventDao
    abstract fun eventRevisionDao(): EventRevisionDao
    abstract fun mediaAssetDao(): MediaAssetDao
    abstract fun eventMediaDao(): EventMediaDao
    abstract fun topicDao(): TopicDao
    abstract fun personDao(): PersonDao
    abstract fun placeDao(): PlaceDao
    abstract fun eventTopicDao(): EventTopicDao
    abstract fun eventPersonDao(): EventPersonDao
    abstract fun eventPlaceDao(): EventPlaceDao
    abstract fun undoCommandDao(): UndoCommandDao
    abstract fun indexJobDao(): DomainIndexJobDao

    companion object {
        const val NAME = "yuyue-domain.db"
        const val VERSION = 1

        @Volatile
        private var instance: YuyueDomainDatabase? = null

        fun get(context: Context): YuyueDomainDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    YuyueDomainDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
    }
}
