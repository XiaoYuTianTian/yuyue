package com.yuyue.app.domain.local

/**
 * Hand-maintained CREATE statements mirroring Room entities for JDBC unit tests.
 * Room schema export is the compile-time contract; keep these in sync.
 */
object DomainSchema {
    const val VERSION = 1

    fun createStatements(): List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS `domain_notebooks` (
          `id` TEXT NOT NULL,
          `name` TEXT NOT NULL,
          `slug` TEXT NOT NULL,
          `color` TEXT,
          `sortOrder` INTEGER NOT NULL,
          `isDefault` INTEGER NOT NULL,
          `archivedAtMillis` INTEGER,
          `createdAtMillis` INTEGER NOT NULL,
          `updatedAtMillis` INTEGER NOT NULL,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `captures` (
          `id` TEXT NOT NULL,
          `notebookId` TEXT,
          `rawText` TEXT NOT NULL,
          `capturedAtMillis` INTEGER NOT NULL,
          `source` TEXT NOT NULL,
          `status` TEXT NOT NULL,
          `mediaJson` TEXT,
          `timezone` TEXT,
          `clientNowMillis` INTEGER,
          `legacyRequestId` TEXT,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_captures_notebookId` ON `captures` (`notebookId`)",
        "CREATE INDEX IF NOT EXISTS `index_captures_status` ON `captures` (`status`)",
        """
        CREATE TABLE IF NOT EXISTS `events` (
          `id` TEXT NOT NULL,
          `notebookId` TEXT NOT NULL,
          `captureId` TEXT,
          `kind` TEXT NOT NULL,
          `status` TEXT NOT NULL,
          `startAtMillis` INTEGER NOT NULL,
          `endAtMillis` INTEGER,
          `statedAtMillis` INTEGER NOT NULL,
          `updatedAtMillis` INTEGER NOT NULL,
          `done` INTEGER NOT NULL,
          `currentRevisionId` TEXT,
          `deletedAtMillis` INTEGER,
          `purgeAfterMillis` INTEGER,
          `timezone` TEXT,
          `legacyPlace` TEXT,
          `legacyPeople` TEXT,
          `legacyDiaryType` TEXT,
          `legacyEntryKind` TEXT,
          `legacySubjectKey` TEXT,
          `legacyPolarity` TEXT,
          `legacyUtteranceId` TEXT,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_events_notebookId` ON `events` (`notebookId`)",
        "CREATE INDEX IF NOT EXISTS `index_events_captureId` ON `events` (`captureId`)",
        "CREATE INDEX IF NOT EXISTS `index_events_startAtMillis` ON `events` (`startAtMillis`)",
        "CREATE INDEX IF NOT EXISTS `index_events_deletedAtMillis` ON `events` (`deletedAtMillis`)",
        """
        CREATE TABLE IF NOT EXISTS `event_revisions` (
          `id` TEXT NOT NULL,
          `eventId` TEXT NOT NULL,
          `parentRevisionId` TEXT,
          `source` TEXT NOT NULL,
          `title` TEXT NOT NULL,
          `body` TEXT NOT NULL,
          `createdAtMillis` INTEGER NOT NULL,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_event_revisions_eventId` ON `event_revisions` (`eventId`)",
        """
        CREATE TABLE IF NOT EXISTS `media_assets` (
          `id` TEXT NOT NULL,
          `sha256` TEXT NOT NULL,
          `mime` TEXT NOT NULL,
          `relativePath` TEXT NOT NULL,
          `thumbRelativePath` TEXT,
          `width` INTEGER NOT NULL,
          `height` INTEGER NOT NULL,
          `createdAtMillis` INTEGER NOT NULL,
          `deletedAtMillis` INTEGER,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_media_assets_sha256` ON `media_assets` (`sha256`)",
        "CREATE INDEX IF NOT EXISTS `index_media_assets_deletedAtMillis` ON `media_assets` (`deletedAtMillis`)",
        """
        CREATE TABLE IF NOT EXISTS `event_media` (
          `eventId` TEXT NOT NULL,
          `assetId` TEXT NOT NULL,
          `sortOrder` INTEGER NOT NULL,
          `role` TEXT NOT NULL,
          PRIMARY KEY(`eventId`, `assetId`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_event_media_assetId` ON `event_media` (`assetId`)",
        """
        CREATE TABLE IF NOT EXISTS `topics` (
          `id` TEXT NOT NULL,
          `canonicalName` TEXT NOT NULL,
          `aliasesJson` TEXT NOT NULL,
          `createdAtMillis` INTEGER NOT NULL,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `people` (
          `id` TEXT NOT NULL,
          `canonicalName` TEXT NOT NULL,
          `aliasesJson` TEXT NOT NULL,
          `createdAtMillis` INTEGER NOT NULL,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `places` (
          `id` TEXT NOT NULL,
          `canonicalName` TEXT NOT NULL,
          `aliasesJson` TEXT NOT NULL,
          `createdAtMillis` INTEGER NOT NULL,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `event_topics` (
          `eventId` TEXT NOT NULL,
          `topicId` TEXT NOT NULL,
          `sortOrder` INTEGER NOT NULL,
          PRIMARY KEY(`eventId`, `topicId`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `event_people` (
          `eventId` TEXT NOT NULL,
          `personId` TEXT NOT NULL,
          `role` TEXT,
          `sortOrder` INTEGER NOT NULL,
          PRIMARY KEY(`eventId`, `personId`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `event_places` (
          `eventId` TEXT NOT NULL,
          `placeId` TEXT NOT NULL,
          `sortOrder` INTEGER NOT NULL,
          PRIMARY KEY(`eventId`, `placeId`)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS `undo_commands` (
          `id` TEXT NOT NULL,
          `createdAtMillis` INTEGER NOT NULL,
          `expiresAtMillis` INTEGER NOT NULL,
          `inverseJson` TEXT NOT NULL,
          `consumedAtMillis` INTEGER,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_undo_commands_expiresAtMillis` ON `undo_commands` (`expiresAtMillis`)",
        """
        CREATE TABLE IF NOT EXISTS `domain_index_jobs` (
          `id` TEXT NOT NULL,
          `eventId` TEXT NOT NULL,
          `jobType` TEXT NOT NULL,
          `status` TEXT NOT NULL,
          `attemptCount` INTEGER NOT NULL,
          `lastError` TEXT,
          `createdAtMillis` INTEGER NOT NULL,
          `updatedAtMillis` INTEGER NOT NULL,
          PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_domain_index_jobs_status` ON `domain_index_jobs` (`status`)",
    )
}
