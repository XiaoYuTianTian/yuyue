package com.yuyue.app.notify

object OnThisDayNotify {
    data class Hit(val year: Int, val title: String)
    data class Content(val title: String, val body: String)

    fun build(hits: List<Hit>): Content? {
        if (hits.isEmpty()) return null
        val title = "那年今日"
        val body = when (hits.size) {
            1 -> {
                val h = hits.first()
                "${h.year}年：${h.title.ifBlank { "一条回忆" }}"
            }
            else -> {
                val first = hits.first()
                "${hits.size} 条回忆 · ${first.year}年「${first.title.ifBlank { "回忆" }}」等"
            }
        }
        return Content(title, body)
    }
}
