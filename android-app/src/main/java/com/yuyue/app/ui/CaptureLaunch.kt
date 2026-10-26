package com.yuyue.app.ui

object CaptureLaunch {
    const val QUICK_ACTION = "com.yuyue.app.action.QUICK_CAPTURE"

    fun isQuickCapture(action: String?): Boolean = action == QUICK_ACTION

    /** Desktop shortcut / widget should open the voice ball, not the keyboard. */
    fun opensVoiceBall(action: String?): Boolean = isQuickCapture(action)
}
