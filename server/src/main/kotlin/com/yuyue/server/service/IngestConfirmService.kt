package com.yuyue.server.service

import com.yuyue.server.repo.EntryRepository
import com.yuyue.server.repo.NotebookRepository
import com.yuyue.server.util.TimeUtil
import kotlinx.serialization.json.*
import java.util.UUID

object IngestConfirmService {
    private val json = Json { ignoreUnknownKeys = true }

    fun confirmFromJobOutput(
        userId: UUID,
        notebookId: UUID,
        outputJson: String,
        idempotencyKey: String?,
    ): List<com.yuyue.server.dto.EntryDto> {
        val root = json.parseToJsonElement(outputJson).jsonObject
        val entries = root["entries"]?.jsonArray ?: return emptyList()
        val saved = mutableListOf<com.yuyue.server.dto.EntryDto>()
        NotebookRepository.getOwned(notebookId, userId)
            ?: throw IllegalArgumentException("notebook not found")

        for (el in entries) {
            val obj = el.jsonObject
            val kind = obj["entryKind"]?.jsonPrimitive?.content ?: "note"
            val title = obj["title"]?.jsonPrimitive?.content ?: "记录"
            val body = obj["body"]?.jsonPrimitive?.content ?: ""
            val startAt = obj["startAt"]?.jsonPrimitive?.content ?: TimeUtil.toIso(java.time.Instant.now())
            val statedAt = obj["statedAt"]?.jsonPrimitive?.content ?: startAt
            val subjectKey = obj["subjectKey"]?.jsonPrimitive?.content
            val polarity = obj["polarity"]?.jsonPrimitive?.content
            val confidence = obj["confidence"]?.jsonPrimitive?.floatOrNull
            val sourceQuote = obj["sourceQuote"]?.jsonPrimitive?.content

            val supersede = obj["supersedesMatch"]?.jsonObject
            var supersedesId: UUID? = null
            if (supersede != null) {
                val sk = supersede["subjectKey"]?.jsonPrimitive?.content
                val pol = supersede["polarity"]?.jsonPrimitive?.content
                val old = EntryRepository.search(
                    userId, listOf(notebookId), null, null,
                    emptyList(), sk?.let { listOf(it) }, listOf("preference"),
                    listOf("active"), false, 5,
                ).firstOrNull { pol == null || it.polarity == pol }
                if (old != null) {
                    EntryRepository.markSuperseded(UUID.fromString(old.id), userId)
                    supersedesId = UUID.fromString(old.id)
                }
            }

            val entry = EntryRepository.insert(
                userId = userId,
                notebookId = notebookId,
                entryKind = kind,
                status = "active",
                title = title,
                body = body,
                rawTranscript = body,
                startAt = TimeUtil.parseInstant(startAt),
                endAt = null,
                statedAt = TimeUtil.parseInstant(statedAt),
                subjectKey = subjectKey,
                polarity = polarity,
                confidence = confidence,
                sourceQuote = sourceQuote,
                idempotencyKey = idempotencyKey,
                supersedesEntryId = supersedesId,
            )
            saved.add(entry)
        }
        return saved
    }

    fun applyMutations(userId: UUID, actions: List<com.yuyue.server.dto.MutationAction>) {
        for (action in actions) {
            val id = UUID.fromString(action.entryId)
            when (action.action) {
                "delete" -> EntryRepository.softDelete(id, userId)
                "cancel" -> EntryRepository.patch(id, userId, com.yuyue.server.dto.PatchEntryRequest(status = "cancelled"))
                else -> {}
            }
        }
    }
}
