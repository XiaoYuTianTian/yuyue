package com.yuyue.app.util

enum class InputIntent { Ingest, Recall, Mutate }

object InputIntents {
    fun classify(text: String): InputIntent {
        val t = text.trim()
        if (t.contains("删") || t.contains("不用") || t.contains("移除") || t.contains("取消这条")) {
            return InputIntent.Mutate
        }
        val recallHints = listOf(
            "做了什么", "干了什么", "有什么安排", "有什么事",
            "回忆", "记得", "哪天", "什么时候",
            "去年", "前年", "上周", "昨天", "查找", "搜一下",
            "有没有", "那次", "当时",
        )
        if (recallHints.any { t.contains(it) } || ScheduleQuery.parse(t) != null && (
                t.contains("什么") || t.contains("哪些") || t.contains("？") || t.contains("?")
                )
        ) {
            return InputIntent.Recall
        }
        return InputIntent.Ingest
    }
}
