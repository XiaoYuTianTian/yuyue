package com.yuyue.app.llm

import kotlinx.serialization.json.Json

object StructuredTurnParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        coerceInputValues = false
    }

    fun parse(raw: String): StructuredTurn {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw StructuredTurnParseException("没有找到 JSON 对象")
        }
        val slice = raw.substring(start, end + 1)
        return try {
            json.decodeFromString(StructuredTurn.serializer(), slice)
        } catch (e: Exception) {
            throw StructuredTurnParseException("无法解析模型返回: ${e.message}", e)
        }
    }
}
