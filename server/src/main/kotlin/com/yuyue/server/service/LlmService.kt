package com.yuyue.server.service

import com.yuyue.server.config.AppConfig
import com.yuyue.server.dto.NotebookDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object LlmService {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class IngestResult(
        val intent: String = "create",
        val entries: List<IngestEntry> = emptyList(),
    )

    @Serializable
    data class IngestEntry(
        val entryKind: String = "note",
        val title: String,
        val body: String,
        val subjectKey: String? = null,
        val polarity: String? = null,
        val startAt: String,
        val statedAt: String? = null,
        val endAt: String? = null,
        val confidence: Float? = null,
        val sourceQuote: String? = null,
        val supersedesMatch: SupersedesMatch? = null,
    )

    @Serializable
    data class SupersedesMatch(
        val subjectKey: String,
        val polarity: String? = null,
    )

    @Serializable
    data class QueryPlan(
        val notebookIds: List<String>? = null,
        val temporalScope: String = "explicit_range",
        val timeRange: TimeRange? = null,
        val subjectKeys: List<String>? = null,
        val keywords: List<String> = emptyList(),
        val entryKinds: List<String>? = null,
        val statusFilter: List<String> = listOf("active"),
        val includeSuperseded: Boolean = false,
        val searchMode: String = "fts",
        val sort: String = "stated_at_desc",
        val limit: Int = 30,
    )

    @Serializable
    data class TimeRange(val startAt: String? = null, val endAt: String? = null)

    @Serializable
    data class MatchPlan(
        val action: String,
        val match: MatchCriteria,
        val reason: String? = null,
    )

    @Serializable
    data class MatchCriteria(
        val keywords: List<String> = emptyList(),
        val notebookIds: List<String>? = null,
        val timeRange: TimeRange? = null,
        val entryKinds: List<String>? = null,
    )

    fun parseIngest(text: String, clientNow: String, notebookName: String): IngestResult {
        if (AppConfig.llmApiKey != null) {
            runCatching { return callLlmIngest(text, clientNow, notebookName) }.onFailure { it.printStackTrace() }
        }
        return mockIngest(text, clientNow)
    }

    fun parseMutate(text: String, clientNow: String): MatchPlan? {
        val lower = text.lowercase()
        if (lower.contains("删") || lower.contains("移除") || lower.contains("不要")) {
            val keywords = mutableListOf<String>()
            if (lower.contains("打卡")) keywords.add("打卡")
            return MatchPlan(
                action = if (lower.contains("删")) "delete" else "cancel",
                match = MatchCriteria(keywords = keywords.ifEmpty { listOf(text.take(8)) }),
                reason = text,
            )
        }
        return null
    }

    fun buildQueryPlan(query: String, clientNow: String, notebooks: List<NotebookDto>): QueryPlan {
        if (AppConfig.llmApiKey != null) {
            runCatching { return callLlmRecall(query, clientNow, notebooks) }.onFailure { it.printStackTrace() }
        }
        return mockQueryPlan(query, clientNow, notebooks)
    }

    fun summarizeRecall(query: String, entriesJson: String): String {
        if (AppConfig.llmApiKey != null) {
            runCatching { return callLlmAnswer(query, entriesJson) }.onFailure { it.printStackTrace() }
        }
        return "共找到相关记录，请查看下方时间线。"
    }

    private fun mockIngest(text: String, clientNow: String): IngestResult {
        val now = clientNow.ifBlank { Instant.now().toString() }
        val lower = text.lowercase()
        val kind = when {
            lower.contains("打卡") || lower.contains("提醒") -> "task"
            lower.contains("喜欢") || lower.contains("不喜欢") -> "preference"
            else -> "note"
        }
        val polarity = when {
            lower.contains("不喜欢") || lower.contains("讨厌") -> "negative"
            lower.contains("喜欢") -> "positive"
            else -> null
        }
        val subject = when {
            lower.contains("水果") || lower.contains("苹果") -> "food/fruit"
            lower.contains("打卡") -> "habit/checkin"
            lower.contains("外卖") -> "food/takeout"
            else -> "general/${text.take(4).hashCode().and(0x7fff)}"
        }
        val title = text.take(24).ifBlank { "语音记录" }
        return IngestResult(
            intent = "create",
            entries = listOf(
                IngestEntry(
                    entryKind = kind,
                    title = title,
                    body = text,
                    subjectKey = subject,
                    polarity = polarity,
                    startAt = now,
                    statedAt = now,
                    confidence = 0.75f,
                    sourceQuote = text.take(120),
                    supersedesMatch = if (polarity == "negative" && subject == "food/fruit") {
                        SupersedesMatch("food/fruit", "positive")
                    } else null,
                ),
            ),
        )
    }

    private fun mockQueryPlan(query: String, clientNow: String, notebooks: List<NotebookDto>): QueryPlan {
        val lower = query.lowercase()
        val scope = when {
            lower.contains("现在") || lower.contains("目前") || lower.contains("当今") -> "as_of_now"
            lower.contains("说过什么") || lower.contains("所有") -> "all_time"
            else -> "explicit_range"
        }
        val keywords = mutableListOf<String>()
        if (lower.contains("水果") || lower.contains("苹果")) keywords.add("水果")
        if (lower.contains("外卖")) keywords.add("外卖")
        if (lower.contains("打卡")) keywords.add("打卡")
        if (keywords.isEmpty()) keywords.add(query.take(6).ifBlank { "记录" })

        val subjectKeys = if (lower.contains("水果")) listOf("food/fruit") else null
        val zoned = ZonedDateTime.parse(clientNow.replace(" ", "T").let {
            if (it.contains("+") || it.endsWith("Z")) it else "${it}+08:00"
        })
        val year = zoned.year
        val range = if (lower.contains("去年")) {
            TimeRange("${year - 1}-01-01T00:00:00+08:00", "${year - 1}-12-31T23:59:59+08:00")
        } else null

        return QueryPlan(
            notebookIds = notebooks.map { it.id },
            temporalScope = scope,
            timeRange = range,
            subjectKeys = subjectKeys,
            keywords = keywords,
            includeSuperseded = scope == "all_time",
            statusFilter = if (scope == "all_time") listOf("active", "superseded") else listOf("active"),
            limit = 30,
        )
    }

    private fun callLlmIngest(text: String, clientNow: String, notebookName: String): IngestResult {
        val prompt = """
            你是语曰(Yuyue)记事助手。根据用户口述整理为JSON。当前时间: $clientNow，记事本: $notebookName。
            只输出JSON: {"intent":"create","entries":[{"entryKind":"event|task|preference|note","title":"","body":"","subjectKey":"","polarity":null,"startAt":"ISO8601","statedAt":"ISO8601","confidence":0.8,"sourceQuote":""}]}
            用户说: $text
        """.trimIndent()
        val content = chatCompletion(prompt) ?: return mockIngest(text, clientNow)
        return extractJson(content)?.let { json.decodeFromString<IngestResult>(it) } ?: mockIngest(text, clientNow)
    }

    private fun callLlmRecall(query: String, clientNow: String, notebooks: List<NotebookDto>): QueryPlan {
        val nb = notebooks.joinToString { "${it.name}(${it.id})" }
        val prompt = """
            生成回忆检索计划JSON。当前: $clientNow。记事本: $nb
            temporal_scope: as_of_now|explicit_range|all_time
            用户问: $query
            输出QueryPlan JSON字段: notebookIds, temporalScope, timeRange, subjectKeys, keywords, entryKinds, includeSuperseded, limit
        """.trimIndent()
        val content = chatCompletion(prompt) ?: return mockQueryPlan(query, clientNow, notebooks)
        return extractJson(content)?.let { json.decodeFromString<QueryPlan>(it) } ?: mockQueryPlan(query, clientNow, notebooks)
    }

    private fun callLlmAnswer(query: String, entriesJson: String): String {
        val prompt = "用户问: $query\n记录: $entriesJson\n用中文简要回答并注明时间。"
        return chatCompletion(prompt) ?: "共找到相关记录。"
    }

    private fun chatCompletion(prompt: String): String? {
        val apiKey = AppConfig.llmApiKey ?: return null
        val body = buildJsonObject {
            put("model", JsonPrimitive(AppConfig.llmModel))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive(prompt))
                })
            })
            put("response_format", buildJsonObject { put("type", JsonPrimitive("json_object")) })
            put("max_tokens", JsonPrimitive(2048))
            // MiniMax M2.x 可能忽略；保留以便其他模型关闭思考链
            put("thinking", buildJsonObject { put("type", JsonPrimitive("disabled")) })
        }.toString()

        val conn = URI("${AppConfig.llmBaseUrl.trimEnd('/')}/v1/chat/completions").toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20_000
        conn.readTimeout = 90_000
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        if (conn.responseCode !in 200..299) {
            val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull()
            println("[Yuyue][LLM] HTTP ${conn.responseCode}: ${err?.take(500)}")
            return null
        }
        val resp = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        val el = json.parseToJsonElement(resp)
        val content = el.jsonObject["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
        return content?.let { stripThinking(it) }
    }

    /** MiniMax 等模型常在 content 前附加 <think>...</think>，需去掉再解析 JSON。 */
    private fun stripThinking(text: String): String =
        text.replace(Regex("(?s)<think>.*?</think>\\s*"), "").trim()

    private fun extractJson(text: String): String? {
        val cleaned = stripThinking(text)
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return cleaned.substring(start, end + 1)
    }
}
