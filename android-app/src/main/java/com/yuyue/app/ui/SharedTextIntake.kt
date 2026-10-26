package com.yuyue.app.ui

object SharedTextIntake {
    sealed class Decision
    data class Submit(val text: String) : Decision()
    data class Hold(val text: String) : Decision()
    data object Ignore : Decision()

    fun decide(notebookId: String?, text: String?): Decision {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return Ignore
        return if (notebookId == null) Hold(t) else Submit(t)
    }
}
