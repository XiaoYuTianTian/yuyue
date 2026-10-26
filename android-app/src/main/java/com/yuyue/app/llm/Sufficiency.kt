package com.yuyue.app.llm

object Sufficiency {
    const val MAX_CLARIFY_ROUNDS = 2

    private val scheduleTypes = setOf("meeting", "work", "travel")

    fun forceSave(userText: String): Boolean {
        val t = userText.trim()
        if (t.isEmpty()) return false
        return t.contains("先记下") ||
            t.contains("不用补") ||
            t.contains("以后再说") ||
            t.contains("以后再补") ||
            t.contains("先记着")
    }

    fun isScheduleType(diaryType: String?): Boolean =
        diaryType?.trim()?.lowercase() in scheduleTypes

    fun diaryTypeOf(turn: StructuredTurn): String =
        turn.entries.firstOrNull()?.diaryType?.trim()?.lowercase().orEmpty().ifBlank { "life" }

    fun missingSlots(turn: StructuredTurn): List<String> {
        val declared = turn.missingSlots.map { it.trim().lowercase() }.filter {
            it == "time" || it == "place" || it == "people"
        }
        val raw = if (declared.isNotEmpty()) {
            declared.distinct()
        } else {
            val e = turn.entries.firstOrNull()
            val missing = mutableListOf<String>()
            if (!turn.timeExplicit) missing += "time"
            if (e == null || e.place.isBlank()) missing += "place"
            if (e == null || e.people.none { it.isNotBlank() }) missing += "people"
            missing
        }
        val filtered = if (isScheduleType(diaryTypeOf(turn))) {
            raw
        } else {
            raw.filter { it == "time" }
        }
        return prioritize(filtered)
    }

    /** 追问优先级：时间 > 地点 > 人物 */
    fun prioritize(slots: List<String>): List<String> {
        val set = slots.map { it.trim().lowercase() }.toHashSet()
        return listOf("time", "place", "people").filter { it in set }
    }

    fun primaryMissing(turn: StructuredTurn): String? =
        prioritize(missingSlots(turn)).firstOrNull()

    fun shouldClarify(turn: StructuredTurn, userText: String, roundsSoFar: Int): Boolean {
        if (turn.intent == TurnIntent.Recall || turn.intent == TurnIntent.Mutate) return false
        if (forceSave(userText)) return false
        if (roundsSoFar >= MAX_CLARIFY_ROUNDS) return false
        val e = turn.entries.firstOrNull()
        val hasContent = e != null && (e.title.isNotBlank() || e.body.isNotBlank())
        if (!isScheduleType(diaryTypeOf(turn))) {
            if (hasContent) return false
            return missingSlots(turn).isNotEmpty()
        }
        return missingSlots(turn).size >= 2
    }

    fun question(turn: StructuredTurn): String {
        // 首轮只问最高优先级一槽，不用模型长问
        return when (primaryMissing(turn)) {
            "time" -> "什么时候？"
            "place" -> "在哪里？"
            "people" -> "和谁一起？"
            else -> turn.clarifyQuestion?.trim()?.takeIf { it.isNotEmpty() } ?: "还需要补充什么？"
        }
    }
}
