package com.yuyue.app.llm

object FastExtractFailure {
    private val httpCode = Regex("""Chat HTTP (\d+)""")

    fun isClientHttpError(error: Throwable): Boolean {
        val code = httpCode.find(error.message.orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: return false
        return code in 400..499
    }

    fun logSafe(error: Throwable): String {
        val code = httpCode.find(error.message.orEmpty())?.groupValues?.get(1)
        return if (code != null) "http=$code" else "extract-failed"
    }
}
