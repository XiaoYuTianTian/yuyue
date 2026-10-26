package com.yuyue.app.ui

object SharedMediaIntake {
    sealed class Decision
    data class SubmitText(val text: String) : Decision()
    data class HoldText(val text: String) : Decision()
    data class SubmitImage(val caption: String?) : Decision()
    data class HoldImage(val caption: String?) : Decision()
    data object Ignore : Decision()

    fun isImageMime(mimeType: String?): Boolean {
        val m = mimeType?.lowercase().orEmpty()
        return m.startsWith("image/")
    }

    fun decide(
        notebookId: String?,
        text: String?,
        mimeType: String?,
        hasStream: Boolean,
    ): Decision {
        val caption = text?.trim()?.takeIf { it.isNotEmpty() }
        if (hasStream && isImageMime(mimeType)) {
            return if (notebookId == null) HoldImage(caption) else SubmitImage(caption)
        }
        if (caption == null) return Ignore
        return if (notebookId == null) HoldText(caption) else SubmitText(caption)
    }
}
