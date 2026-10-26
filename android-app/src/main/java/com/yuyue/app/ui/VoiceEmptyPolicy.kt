package com.yuyue.app.ui

object VoiceEmptyPolicy {
    fun message(transcript: String, framesHeard: Boolean): String? {
        val t = transcript.trim()
        if (t.isNotEmpty()) return null
        return if (framesHeard) "没听清，请再按住说一次" else "没有听到声音"
    }
}
