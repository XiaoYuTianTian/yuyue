package com.yuyue.app.ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object PersistentUndoCodec {
    const val TTL_MILLIS: Long = 24L * 60L * 60L * 1000L

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Envelope(
        val kind: String,
        val id: String,
        val previous: Boolean? = null,
        val createdAtMillis: Long,
    )

    fun encode(action: UndoAction, createdAtMillis: Long = System.currentTimeMillis()): String {
        val env = when (action) {
            is UndoAction.SoftDelete -> Envelope("soft_delete", action.id, createdAtMillis = createdAtMillis)
            is UndoAction.ToggleDone -> Envelope(
                "toggle_done",
                action.id,
                previous = action.previous,
                createdAtMillis = createdAtMillis,
            )
        }
        return json.encodeToString(Envelope.serializer(), env)
    }

    fun decode(raw: String?): UndoAction? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val env = json.decodeFromString(Envelope.serializer(), raw)
            when (env.kind) {
                "soft_delete" -> UndoAction.SoftDelete(env.id)
                "toggle_done" -> UndoAction.ToggleDone(env.id, env.previous == true)
                else -> null
            }
        }.getOrNull()
    }

    fun decodeIfFresh(raw: String?, nowMillis: Long): UndoAction? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val env = json.decodeFromString(Envelope.serializer(), raw)
            if (nowMillis - env.createdAtMillis > TTL_MILLIS) return null
            when (env.kind) {
                "soft_delete" -> UndoAction.SoftDelete(env.id)
                "toggle_done" -> UndoAction.ToggleDone(env.id, env.previous == true)
                else -> null
            }
        }.getOrNull()
    }
}
