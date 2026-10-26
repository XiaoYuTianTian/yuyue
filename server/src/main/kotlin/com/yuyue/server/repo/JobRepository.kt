package com.yuyue.server.repo

import com.yuyue.server.db.AsyncJobsTable
import com.yuyue.server.dto.JobDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

object JobRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(userId: UUID, type: String, input: String): UUID = transaction {
        val id = UUID.randomUUID()
        val now = Instant.now()
        AsyncJobsTable.insert {
            it[AsyncJobsTable.id] = id
            it[AsyncJobsTable.userId] = userId
            it[AsyncJobsTable.type] = type
            it[status] = "pending"
            it[AsyncJobsTable.input] = input
            it[createdAt] = now
        }
        id
    }

    fun claimPending(): Pair<UUID, UUID>? = transaction {
        val row = AsyncJobsTable.selectAll()
            .where { AsyncJobsTable.status eq "pending" }
            .orderBy(AsyncJobsTable.createdAt to SortOrder.ASC)
            .limit(1)
            .firstOrNull() ?: return@transaction null
        val jobId = row[AsyncJobsTable.id].value
        AsyncJobsTable.update({ AsyncJobsTable.id eq jobId }) {
            it[status] = "running"
        }
        jobId to row[AsyncJobsTable.userId].value
    }

    fun complete(jobId: UUID, output: String?) = transaction {
        AsyncJobsTable.update({ AsyncJobsTable.id eq jobId }) {
            it[status] = "succeeded"
            it[AsyncJobsTable.output] = output
            it[completedAt] = Instant.now()
        }
    }

    fun fail(jobId: UUID, error: String) = transaction {
        AsyncJobsTable.update({ AsyncJobsTable.id eq jobId }) {
            it[status] = "failed"
            it[AsyncJobsTable.error] = error
            it[completedAt] = Instant.now()
        }
    }

    fun getRawOutput(jobId: UUID): String? = transaction {
        AsyncJobsTable.selectAll()
            .where { AsyncJobsTable.id eq jobId }
            .firstOrNull()?.get(AsyncJobsTable.output)
    }

    fun getRawInput(jobId: UUID): String? = transaction {
        AsyncJobsTable.selectAll()
            .where { AsyncJobsTable.id eq jobId }
            .firstOrNull()?.get(AsyncJobsTable.input)
    }

    fun get(jobId: UUID, userId: UUID): JobDto? = transaction {
        AsyncJobsTable.selectAll()
            .where { (AsyncJobsTable.id eq jobId) and (AsyncJobsTable.userId eq userId) }
            .firstOrNull()?.let { row ->
                val outputStr = row[AsyncJobsTable.output]
                val outputEl: JsonElement? = outputStr?.let {
                    runCatching { json.parseToJsonElement(it) }.getOrNull()
                }
                JobDto(
                    id = row[AsyncJobsTable.id].value.toString(),
                    type = row[AsyncJobsTable.type],
                    status = row[AsyncJobsTable.status],
                    output = outputEl,
                    error = row[AsyncJobsTable.error],
                )
            }
    }
}
