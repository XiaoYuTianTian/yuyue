package com.yuyue.app.ui

import android.content.Context

/** Survives process death; Domain UndoCommand remains for Stage-2 domain DB. */
class PersistentUndoStore(context: Context) {
    private val prefs = context.getSharedPreferences("yuyue_undo", Context.MODE_PRIVATE)

    fun save(action: UndoAction) {
        prefs.edit()
            .putString(KEY, PersistentUndoCodec.encode(action))
            .apply()
    }

    fun peek(nowMillis: Long = System.currentTimeMillis()): UndoAction? {
        val action = PersistentUndoCodec.decodeIfFresh(prefs.getString(KEY, null), nowMillis)
        if (action == null) clear()
        return action
    }

    fun consume(nowMillis: Long = System.currentTimeMillis()): UndoAction? {
        val action = peek(nowMillis) ?: return null
        clear()
        return action
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "last_undo"
    }
}
