package com.yuyue.app.agent

import com.yuyue.app.data.AiConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatContentPart(
    val type: String,
    val text: String? = null,
    val imageUrl: String? = null,
)

data class ChatMessage(
    val role: String,
    val content: String? = null,
    val toolCalls: JsonArray? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    /** 完整 assistant 原始 message 对象，便于回传 thinking / tool_calls */
    val raw: JsonObject? = null,
    val contentParts: List<ChatContentPart>? = null,
)

data class ChatCompletionResult(
    val message: ChatMessage,
    val finishReason: String?,
)

class MiniMaxChatClient(
    private val config: AiConfigStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun chatTextOnly(
        messages: List<ChatMessage>,
        modelOverride: String? = null,
        baseUrlOverride: String? = null,
        apiKeyOverride: String? = null,
        maxTokens: Int = 4096,
        includeThinking: Boolean = true,
        timeoutMs: Long? = null,
    ): ChatCompletionResult = chat(
        messages = messages,
        tools = null,
        modelOverride = modelOverride,
        baseUrlOverride = baseUrlOverride,
        apiKeyOverride = apiKeyOverride,
        maxTokens = maxTokens,
        includeThinking = includeThinking,
        timeoutMs = timeoutMs,
    )

    suspend fun chat(
        messages: List<ChatMessage>,
        tools: JsonArray? = null,
        toolChoice: String = "auto",
        modelOverride: String? = null,
        baseUrlOverride: String? = null,
        apiKeyOverride: String? = null,
        maxTokens: Int = 4096,
        includeThinking: Boolean = true,
        timeoutMs: Long? = null,
    ): ChatCompletionResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOverride?.takeIf { it.isNotBlank() } ?: config.apiKey
        require(apiKey.isNotBlank()) { "模型密钥未内置，请检查安装包" }

        val body = buildJsonObject {
            put("model", modelOverride?.takeIf { it.isNotBlank() } ?: config.model)
            put("messages", messagesToJson(messages))
            if (tools != null) {
                put("tools", tools)
                put("tool_choice", toolChoice)
            }
            put("max_tokens", maxTokens)
            if (includeThinking) {
                put("thinking", buildJsonObject { put("type", "disabled") })
            }
        }.toString()

        val base = (baseUrlOverride ?: config.baseUrl).trimEnd('/')
        val url = "$base/v1/chat/completions"
        val http = if (timeoutMs != null) {
            client.newBuilder()
                .connectTimeout(timeoutMs.coerceAtMost(5_000L), TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMs + 1_000L, TimeUnit.MILLISECONDS)
                .build()
        } else {
            client
        }
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Chat HTTP ${resp.code}: ${raw.take(500)}")
            }
            parseCompletion(raw)
        }
    }

    private fun messagesToJson(messages: List<ChatMessage>): JsonArray = buildJsonArray {
        for (m in messages) {
            if (m.raw != null && m.role == "assistant") {
                add(m.raw)
                continue
            }
            add(
                buildJsonObject {
                    put("role", m.role)
                    when (m.role) {
                        "tool" -> {
                            put("tool_call_id", m.toolCallId ?: "")
                            put("content", m.content ?: "")
                            m.name?.let { put("name", it) }
                        }
                        else -> {
                            put("content", contentToJson(m))
                            m.toolCalls?.let { put("tool_calls", it) }
                        }
                    }
                },
            )
        }
    }

    private fun contentToJson(message: ChatMessage): kotlinx.serialization.json.JsonElement {
        val parts = message.contentParts
        if (parts.isNullOrEmpty()) return JsonPrimitive(message.content ?: "")
        return buildJsonArray {
            for (part in parts) {
                add(
                    buildJsonObject {
                        put("type", part.type)
                        when (part.type) {
                            "image_url" -> put(
                                "image_url",
                                buildJsonObject {
                                    put("url", part.imageUrl ?: "")
                                    put("detail", "default")
                                },
                            )
                            else -> put("text", part.text ?: "")
                        }
                    },
                )
            }
        }
    }

    private fun parseCompletion(raw: String): ChatCompletionResult {
        val root = json.parseToJsonElement(raw).jsonObject
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw IllegalStateException("无 choices: ${raw.take(300)}")
        val msgObj = choice["message"]?.jsonObject
            ?: throw IllegalStateException("无 message")
        val contentRaw = msgObj["content"]?.jsonPrimitive?.contentOrNull
        val content = contentRaw?.let { stripThinking(it) }
        val toolCalls = msgObj["tool_calls"] as? JsonArray
        return ChatCompletionResult(
            message = ChatMessage(
                role = "assistant",
                content = content,
                toolCalls = toolCalls,
                raw = msgObj,
            ),
            finishReason = choice["finish_reason"]?.jsonPrimitive?.contentOrNull,
        )
    }

    companion object {
        fun stripThinking(text: String): String =
            text.replace(Regex("(?s)<think>.*?</think>\\s*"), "").trim()
    }
}
