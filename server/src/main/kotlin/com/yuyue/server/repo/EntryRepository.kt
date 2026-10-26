package com.yuyue.server.repo

import com.yuyue.server.db.EntriesTable
import com.yuyue.server.dto.EntryDto
import com.yuyue.server.dto.PatchEntryRequest
import com.yuyue.server.util.TimeUtil
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

object EntryRepository {
    fun toDto(row: ResultRow) = EntryDto(
        id = row[EntriesTable.id].value.toString(),
        notebookId = row[EntriesTable.notebookId].value.toString(),
        entryKind = row[EntriesTable.entryKind],
        status = row[EntriesTable.status],
        subjectKey = row[EntriesTable.subjectKey],
        polarity = row[EntriesTable.polarity],
        supersedesEntryId = row[EntriesTable.supersedesEntryId]?.value?.toString(),
        title = row[EntriesTable.title],
        body = row[EntriesTable.body],
        rawTranscript = row[EntriesTable.rawTranscript],
        startAt = TimeUtil.toIso(row[EntriesTable.startAt]),
        endAt = row[EntriesTable.endAt]?.let { TimeUtil.toIso(it) },
        statedAt = TimeUtil.toIso(row[EntriesTable.statedAt]),
        confidence = row[EntriesTable.confidence],
        sourceQuote = row[EntriesTable.sourceQuote],
        updatedAt = TimeUtil.toIso(row[EntriesTable.updatedAt]),
    )

    fun listActive(
        userId: UUID,
        notebookId: UUID?,
        from: Instant?,
        to: Instant?,
        includeCancelled: Boolean = false,
    ): List<EntryDto> = transaction {
        var cond: Op<Boolean> = (EntriesTable.userId eq userId) and EntriesTable.deletedAt.isNull()
        if (!includeCancelled) {
            cond = cond and (EntriesTable.status eq "active")
        }
        notebookId?.let { cond = cond and (EntriesTable.notebookId eq it) }
        from?.let { cond = cond and (EntriesTable.startAt greaterEq it) }
        to?.let { cond = cond and (EntriesTable.startAt less it) }

        EntriesTable.selectAll().where { cond }
            .orderBy(EntriesTable.startAt to SortOrder.ASC)
            .map(::toDto)
    }

    fun listTrash(userId: UUID): List<EntryDto> = transaction {
        EntriesTable.selectAll()
            .where { (EntriesTable.userId eq userId) and EntriesTable.deletedAt.isNotNull() }
            .orderBy(EntriesTable.deletedAt to SortOrder.DESC)
            .map(::toDto)
    }

    fun get(id: UUID, userId: UUID): EntryDto? = transaction {
        EntriesTable.selectAll()
            .where { (EntriesTable.id eq id) and (EntriesTable.userId eq userId) }
            .firstOrNull()?.let(::toDto)
    }

    fun insert(
        userId: UUID,
        notebookId: UUID,
        entryKind: String,
        status: String,
        title: String,
        body: String,
        rawTranscript: String?,
        startAt: Instant,
        endAt: Instant?,
        statedAt: Instant,
        subjectKey: String?,
        polarity: String?,
        confidence: Float?,
        sourceQuote: String?,
        idempotencyKey: String?,
        supersedesEntryId: UUID? = null,
    ): EntryDto = transaction {
        val now = Instant.now()
        val id = UUID.randomUUID()
        EntriesTable.insert {
            it[EntriesTable.id] = id
            it[EntriesTable.userId] = userId
            it[EntriesTable.notebookId] = notebookId
            it[EntriesTable.entryKind] = entryKind
            it[EntriesTable.status] = status
            it[EntriesTable.subjectKey] = subjectKey
            it[EntriesTable.polarity] = polarity
            it[EntriesTable.supersedesEntryId] = supersedesEntryId
            it[EntriesTable.title] = title
            it[EntriesTable.body] = body
            it[EntriesTable.rawTranscript] = rawTranscript
            it[EntriesTable.startAt] = startAt
            it[EntriesTable.endAt] = endAt
            it[EntriesTable.statedAt] = statedAt
            it[EntriesTable.confidence] = confidence
            it[EntriesTable.sourceQuote] = sourceQuote
            it[EntriesTable.idempotencyKey] = idempotencyKey
            it[EntriesTable.createdAt] = now
            it[EntriesTable.updatedAt] = now
        }
        get(id, userId)!!
    }

    fun patch(id: UUID, userId: UUID, req: PatchEntryRequest): EntryDto? = transaction {
        val row = EntriesTable.selectAll()
            .where { (EntriesTable.id eq id) and (EntriesTable.userId eq userId) }
            .firstOrNull() ?: return@transaction null
        req.updatedAt?.let { clientUpdated ->
            val serverUpdated = row[EntriesTable.updatedAt]
            if (TimeUtil.parseInstant(clientUpdated).isBefore(serverUpdated)) {
                return@transaction get(id, userId)
            }
        }
        val now = Instant.now()
        EntriesTable.update({ EntriesTable.id eq id }) {
            req.title?.let { v -> it[title] = v }
            req.body?.let { v -> it[body] = v }
            req.notebookId?.let { v -> it[notebookId] = UUID.fromString(v) }
            req.startAt?.let { v -> it[startAt] = TimeUtil.parseInstant(v) }
            req.endAt?.let { v -> it[endAt] = TimeUtil.parseInstant(v) }
            req.status?.let { v -> it[status] = v }
            it[updatedAt] = now
        }
        get(id, userId)
    }

    fun softDelete(id: UUID, userId: UUID): Boolean = transaction {
        EntriesTable.update({
            (EntriesTable.id eq id) and (EntriesTable.userId eq userId)
        }) {
            it[deletedAt] = Instant.now()
            it[updatedAt] = Instant.now()
        } > 0
    }

    fun restore(id: UUID, userId: UUID): Boolean = transaction {
        EntriesTable.update({
            (EntriesTable.id eq id) and (EntriesTable.userId eq userId)
        }) {
            it[deletedAt] = null
            it[updatedAt] = Instant.now()
        } > 0
    }

    fun search(
        userId: UUID,
        notebookIds: List<UUID>?,
        from: Instant?,
        to: Instant?,
        keywords: List<String>,
        subjectKeys: List<String>?,
        entryKinds: List<String>?,
        statuses: List<String>,
        includeSuperseded: Boolean,
        limit: Int,
    ): List<EntryDto> = transaction {
        var cond: Op<Boolean> = (EntriesTable.userId eq userId) and EntriesTable.deletedAt.isNull()
        if (!includeSuperseded) {
            cond = cond and (EntriesTable.status inList statuses)
        } else {
            cond = cond and (EntriesTable.status inList statuses + listOf("superseded"))
        }
        notebookIds?.takeIf { it.isNotEmpty() }?.let {
            cond = cond and (EntriesTable.notebookId inList it)
        }
        from?.let { cond = cond and (EntriesTable.statedAt greaterEq it) }
        to?.let { cond = cond and (EntriesTable.statedAt lessEq it) }
        subjectKeys?.takeIf { it.isNotEmpty() }?.let {
            cond = cond and (EntriesTable.subjectKey inList it)
        }
        entryKinds?.takeIf { it.isNotEmpty() }?.let {
            cond = cond and (EntriesTable.entryKind inList it)
        }

        var query = EntriesTable.selectAll().where { cond }
        if (keywords.isNotEmpty()) {
            val keywordCond = keywords.map { kw ->
                val p = "%$kw%"
                (EntriesTable.title like p) or
                    (EntriesTable.body like p) or
                    (EntriesTable.rawTranscript like p)
            }.reduce { a, b -> a or b }
            query = query.andWhere { keywordCond }
        }
        query.orderBy(EntriesTable.statedAt to SortOrder.DESC)
            .limit(limit)
            .map(::toDto)
    }

    fun markSuperseded(oldId: UUID, userId: UUID) = transaction {
        EntriesTable.update({
            (EntriesTable.id eq oldId) and (EntriesTable.userId eq userId)
        }) {
            it[status] = "superseded"
            it[updatedAt] = Instant.now()
        }
    }

    fun findByIdempotency(userId: UUID, key: String): EntryDto? = transaction {
        EntriesTable.selectAll()
            .where { (EntriesTable.userId eq userId) and (EntriesTable.idempotencyKey eq key) }
            .firstOrNull()?.let(::toDto)
    }
}
