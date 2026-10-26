package com.yuyue.app.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class FastExtractResult(
    val intent: TurnIntent,
    val timeExplicit: Boolean,
    val entries: List<StructuredEntry>,
)

@Serializable
private data class FastExtractEntryDto(
    @SerialName("start_at") val startAt: String? = null,
    val title: String = "",
    val body: String = "",
    val raw: String = "",
)

@Serializable
private data class FastExtractDto(
    val intent: String = "record",
    @SerialName("time_explicit") val timeExplicit: Boolean = false,
    @SerialName("start_at") val startAt: String? = null,
    val title: String = "",
    val body: String = "",
    val raw: String = "",
    val entries: List<FastExtractEntryDto> = emptyList(),
)

object FastExtractParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(rawJson: String, nowIso: String, userText: String): FastExtractResult {
        val start = rawJson.indexOf('{')
        val end = rawJson.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw StructuredTurnParseException("没有找到 JSON 对象")
        }
        val dto = try {
            json.decodeFromString(FastExtractDto.serializer(), rawJson.substring(start, end + 1))
        } catch (e: Exception) {
            throw StructuredTurnParseException("无法解析提取结果: ${e.message}", e)
        }
        val parsed = when (dto.intent.trim().lowercase()) {
            "recall" -> TurnIntent.Recall
            "mutate" -> TurnIntent.Mutate
            "record" -> TurnIntent.Record
            else -> null
        }
        val routed = LocalRouter.classify(userText)
        // Weak models often default to record. Local keywords win for recall/mutate
        // so a 7B extract step cannot silently insert a new diary.
        val intent = when {
            parsed == TurnIntent.Recall || parsed == TurnIntent.Mutate -> parsed
            routed == RouteKind.Recall -> TurnIntent.Recall
            routed == RouteKind.Mutate -> TurnIntent.Mutate
            else -> TurnIntent.Record
        }
        if (intent != TurnIntent.Record) {
            return FastExtractResult(intent = intent, timeExplicit = dto.timeExplicit, entries = emptyList())
        }
        val startAt = dto.startAt?.trim().orEmpty().ifBlank { nowIso }
        val title = dto.title.trim().ifBlank { userText.trim().take(20).ifBlank { "日记" } }.take(20)
        val body = dto.body.trim().ifBlank { userText.trim() }
        val raw = dto.raw.trim().ifBlank { userText }
        val single = StructuredEntry(
            title = title,
            startAt = startAt,
            body = body,
            raw = raw,
        )
        val fromDto = dto.entries.mapNotNull { row ->
            val partRaw = row.raw.trim().ifBlank { row.body.trim() }
            if (partRaw.isBlank() && row.title.isBlank()) return@mapNotNull null
            StructuredEntry(
                title = row.title.trim().ifBlank { partRaw.take(20) }.take(20),
                startAt = row.startAt?.trim().orEmpty().ifBlank { nowIso },
                body = row.body.trim().ifBlank { partRaw },
                raw = partRaw.ifBlank { userText },
            )
        }
        val split = MultiEventSplit.entries(userText, nowIso)
        val entries = when {
            fromDto.size >= 2 -> fromDto
            split.size >= 2 -> split
            else -> listOf(single)
        }
        return FastExtractResult(
            intent = TurnIntent.Record,
            timeExplicit = dto.timeExplicit || entries.size > 1,
            entries = entries,
        )
    }

    fun photoOnly(nowIso: String): FastExtractResult = FastExtractResult(
        intent = TurnIntent.Record,
        timeExplicit = false,
        entries = listOf(
            StructuredEntry(
                title = "照片",
                startAt = nowIso,
                body = "",
                raw = "",
            ),
        ),
    )
}
