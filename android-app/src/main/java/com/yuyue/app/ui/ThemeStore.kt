package com.yuyue.app.ui

import android.content.Context

enum class ThemeMode { System, Light, Dark }

class ThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences("yuyue_ui", Context.MODE_PRIVATE)

    var mode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString(KEY, ThemeMode.System.name)!!) }
            .getOrDefault(ThemeMode.System)
        set(value) {
            prefs.edit().putString(KEY, value.name).apply()
        }

    companion object {
        private const val KEY = "theme_mode"
    }
}
