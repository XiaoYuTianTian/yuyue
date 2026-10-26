package com.yuyue.server.repo

import com.yuyue.server.db.NotebooksTable
import com.yuyue.server.dto.NotebookDto
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

object NotebookRepository {
    fun seedDefaults(userId: UUID) {
        val now = Instant.now()
        listOf(
            Triple("生活", "life", true),
            Triple("工作", "work", false),
        ).forEachIndexed { index, (name, slug, default) ->
            NotebooksTable.insert {
                it[id] = UUID.randomUUID()
                it[NotebooksTable.userId] = userId
                it[NotebooksTable.name] = name
                it[NotebooksTable.slug] = slug
                it[sortOrder] = index
                it[isDefault] = default
                it[color] = if (slug == "life") "#4A7C59" else "#5C6BC0"
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    fun listForUser(userId: UUID): List<NotebookDto> = transaction {
        NotebooksTable.selectAll()
            .where { (NotebooksTable.userId eq userId) and NotebooksTable.archivedAt.isNull() }
            .orderBy(NotebooksTable.sortOrder to SortOrder.ASC)
            .map { row ->
                NotebookDto(
                    id = row[NotebooksTable.id].value.toString(),
                    name = row[NotebooksTable.name],
                    slug = row[NotebooksTable.slug],
                    color = row[NotebooksTable.color],
                    icon = row[NotebooksTable.icon],
                    sortOrder = row[NotebooksTable.sortOrder],
                    isDefault = row[NotebooksTable.isDefault],
                )
            }
    }

    fun getOwned(notebookId: UUID, userId: UUID): UUID? = transaction {
        NotebooksTable.selectAll()
            .where { (NotebooksTable.id eq notebookId) and (NotebooksTable.userId eq userId) }
            .firstOrNull()?.get(NotebooksTable.id)?.value
    }
}
