package com.yuyue.app.observability

import android.util.Log

/** Never log diary body, titles, or transcripts. */
object PrivacyLog {
    private const val TAG = "YuyueQuality"

    fun event(name: String, extras: Map<String, String> = emptyMap()) {
        val safe = extras.filterKeys { it != "body" && it != "text" && it != "transcript" }
        Log.i(TAG, buildString {
            append(name)
            if (safe.isNotEmpty()) {
                append(' ')
                append(safe.entries.joinToString(",") { "${it.key}=${it.value}" })
            }
        })
    }
}

object QualityCounters {
    @Volatile var captureOk: Int = 0
    @Volatile var captureFail: Int = 0
    @Volatile var backupOk: Int = 0
    @Volatile var modelFail: Int = 0
}
