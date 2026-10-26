package com.yuyue.app.llm

object DonePolicy {
    fun isDone(startAtMillis: Long, nowMillis: Long): Boolean = startAtMillis <= nowMillis
}
