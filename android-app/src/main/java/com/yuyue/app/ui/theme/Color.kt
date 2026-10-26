package com.yuyue.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 纸 · 墨 · 苔 · 陶
 *
 * Paper = 页底与纸面；Ink = 正文；Moss = 主操作/语音；Clay = 次级强调。
 * 刻意避开通用 Signal 青与紫粉渐变。
 */

val Paper = Color(0xFFEEF1EC)
val PaperRaised = Color(0xFFF7F8F5)
val PaperDim = Color(0xFFE2E6DF)

val Ink = Color(0xFF1A1F1C)
val InkSoft = Color(0xFF3D463F)
val InkMute = Color(0xFF6B746C)

val Moss = Color(0xFF3F6B52)
val MossDeep = Color(0xFF2B4A38)
val MossMist = Color(0xFFD7E5DB)

val Clay = Color(0xFF9A6B4F)
val ClayDeep = Color(0xFF6E4A36)
val ClayMist = Color(0xFFF0E4DA)

val Hazard = Color(0xFFB54A3C)
val HazardMist = Color(0xFFF5DDD9)

val NightPaper = Color(0xFF121612)
val NightRaised = Color(0xFF1B211C)
val NightHigh = Color(0xFF262E27)
val NightInk = Color(0xFFE6EBE6)
val NightMute = Color(0xFF9AA59B)

/** @deprecated 兼容旧引用；新代码用 [Moss] */
@Deprecated("Use Moss", ReplaceWith("Moss"))
val Signal = Moss

@Deprecated("Use MossDeep", ReplaceWith("MossDeep"))
val SignalDeep = MossDeep

@Deprecated("Use Clay", ReplaceWith("Clay"))
val Pulse = Clay

@Deprecated("Use Hazard", ReplaceWith("Hazard"))
val Warn = Hazard

@Deprecated("Use Hazard", ReplaceWith("Hazard"))
val WarnDeep = Hazard

@Deprecated("Use NightPaper", ReplaceWith("NightPaper"))
val Navy = NightPaper

@Deprecated("Use NightRaised", ReplaceWith("NightRaised"))
val NavyPanel = NightRaised

@Deprecated("Use NightHigh", ReplaceWith("NightHigh"))
val NavyRaised = NightHigh

@Deprecated("Use NightInk", ReplaceWith("NightInk"))
val Ice = NightInk

@Deprecated("Use NightMute", ReplaceWith("NightMute"))
val Slate = NightMute

@Deprecated("Use Paper", ReplaceWith("Paper"))
val CoolBg = Paper

@Deprecated("Use Ink", ReplaceWith("Ink"))
val CoolInk = Ink

@Deprecated("Use InkMute", ReplaceWith("InkMute"))
val CoolMuted = InkMute

val ColorScheme.done: Color get() = primary
val ColorScheme.pending: Color get() = tertiary

fun parseNotebookColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Moss
    return runCatching {
        val h = hex.removePrefix("#")
        Color(0xFF000000 or h.toLong(16))
    }.getOrDefault(Moss)
}
