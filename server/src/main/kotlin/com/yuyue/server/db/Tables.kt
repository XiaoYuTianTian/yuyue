package com.yuyue.server.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : UUIDTable("users") {
    val phoneHash = varchar("phone_hash", 128).uniqueIndex()
    val phoneMasked = varchar("phone_masked", 32)
    val timezone = varchar("timezone", 64).default("Asia/Shanghai")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object NotebooksTable : UUIDTable("notebooks") {
    val userId = reference("user_id", UsersTable)
    val name = varchar("name", 128)
    val slug = varchar("slug", 64)
    val color = varchar("color", 32).nullable()
    val icon = varchar("icon", 64).nullable()
    val sortOrder = integer("sort_order").default(0)
    val isDefault = bool("is_default").default(false)
    val archivedAt = timestamp("archived_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object EntriesTable : UUIDTable("entries") {
    val userId = reference("user_id", UsersTable)
    val notebookId = reference("notebook_id", NotebooksTable)
    val entryKind = varchar("entry_kind", 32).default("note")
    val status = varchar("status", 32).default("active")
    val subjectKey = varchar("subject_key", 128).nullable()
    val polarity = varchar("polarity", 16).nullable()
    val supersedesEntryId = reference("supersedes_entry_id", EntriesTable).nullable()
    val title = varchar("title", 512)
    val body = text("body")
    val rawTranscript = text("raw_transcript").nullable()
    val startAt = timestamp("start_at")
    val endAt = timestamp("end_at").nullable()
    val statedAt = timestamp("stated_at")
    val confidence = float("confidence").nullable()
    val sourceQuote = text("source_quote").nullable()
    val schemaVersion = integer("schema_version").default(1)
    val idempotencyKey = varchar("idempotency_key", 128).nullable()
    val deletedAt = timestamp("deleted_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object AsyncJobsTable : UUIDTable("async_jobs") {
    val userId = reference("user_id", UsersTable)
    val type = varchar("type", 32)
    val status = varchar("status", 32).default("pending")
    val input = text("input")
    val output = text("output").nullable()
    val error = text("error").nullable()
    val createdAt = timestamp("created_at")
    val completedAt = timestamp("completed_at").nullable()
}

object UserDailyUsageTable : Table("user_daily_usage") {
    val userId = reference("user_id", UsersTable)
    val usageDate = date("usage_date")
    val ingestCount = integer("ingest_count").default(0)
    val recallCount = integer("recall_count").default(0)
    val llmTokens = integer("llm_tokens").default(0)
    override val primaryKey = PrimaryKey(userId, usageDate)
}

object SmsOtpTable : Table("sms_otp") {
    val phoneHash = varchar("phone_hash", 128)
    val codeHash = varchar("code_hash", 128)
    val expiresAt = timestamp("expires_at")
    val attemptCount = integer("attempt_count").default(0)
    val createdAt = timestamp("created_at")
}
