package com.yuyue.app.ui

object MicPermissionGate {
    fun shouldPrompt(granted: Boolean): Boolean = !granted
}
