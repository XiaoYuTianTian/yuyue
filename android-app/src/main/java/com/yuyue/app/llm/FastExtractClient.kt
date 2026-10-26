package com.yuyue.app.llm

import com.yuyue.app.agent.ChatMessage
import com.yuyue.app.agent.MiniMaxChatClient
import com.yuyue.app.data.AiConfigStore
import kotlinx.coroutines.withTimeoutOrNull

class FastExtractClient(
    private val chat: MiniMaxChatClient,
    private val config: AiConfigStore,
) {
    suspend fun extract(
        nowIso: String,
        userText: String,
        notebookName: String,
        hasImages: Boolean,
    ): FastExtractResult? {
        val text = userText.trim()
        if (text.isBlank()) {
            return if (hasImages) FastExtractParser.photoOnly(nowIso) else null
        }
        if (!config.hasFastApiKey()) return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            runCatching {
                val messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt(nowIso, notebookName)),
                    ChatMessage(role = "user", content = text),
                )
                val first = chat.chatTextOnly(
                    messages = messages,
                    modelOverride = config.fastModel,
                    baseUrlOverride = config.fastBaseUrl,
                    apiKeyOverride = config.siliconflowApiKey,
                    maxTokens = 512,
                    includeThinking = false,
                    timeoutMs = TIMEOUT_MS,
                )
                runCatching {
                    FastExtractParser.parse(first.message.content.orEmpty(), nowIso, text)
                }.getOrElse {
                    val retry = messages + first.message + ChatMessage(
                        role = "user",
                        content = "只输出合法 JSON，不要其它文字。",
                    )
                    val second = chat.chatTextOnly(
                        messages = retry,
                        modelOverride = config.fastModel,
                        baseUrlOverride = config.fastBaseUrl,
                        apiKeyOverride = config.siliconflowApiKey,
                        maxTokens = 512,
                        includeThinking = false,
                        timeoutMs = TIMEOUT_MS,
                    )
                    FastExtractParser.parse(second.message.content.orEmpty(), nowIso, text)
                }
            }.getOrElse { err ->
                if (FastExtractFailure.isClientHttpError(err)) {
                    android.util.Log.w("FastExtract", FastExtractFailure.logSafe(err))
                    null
                } else {
                    throw err
                }
            }
        }
    }

    companion object {
        const val TIMEOUT_MS = 8_000L

        fun systemPrompt(nowIso: String, notebookName: String): String = buildString {
            appendLine("你是语曰的快速提取器。只输出一个 JSON 对象，不要解释。")
            appendLine("当前时间（Asia/Shanghai）: $nowIso")
            appendLine("当前记事本: $notebookName")
            appendLine(
                """
{"intent":"record|recall|mutate","time_explicit":false,"start_at":"ISO8601含时区","title":"≤20字","body":"一两句事实","raw":"用户原话"}
                """.trimIndent(),
            )
            appendLine("intent：新记用 record；查过去用 recall；改/删已有条目用 mutate。拿不准用 record。")
            appendLine("不要写完整日记，不要识图，不要编造没说的地点和人物。")
            appendLine("time_explicit：用户是否说出了日期或相对日（昨天/明天/周三）。没说则为 false。")
            appendLine("说清了日期：start_at 用那天（有时刻用说出的时刻，没有就用当前时刻）。")
            appendLine("没说清日期：start_at 用当前时刻，禁止 00:00。")
            appendLine("title 不超过 20 个汉字。body 一两句事实，可以接近原话。raw 填用户原话。")
        }
    }
}
