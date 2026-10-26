package com.yuyue.app.llm

class StructuredTurnValidationException(message: String) : IllegalArgumentException(message)

object StructuredTurnValidator {
    private val diaryTypes = setOf("life", "work", "food", "meeting", "travel", "health", "other", "task", "plan")
    private val mutateActions = setOf(
        "delete",
        "set_done",
        "set_undone",
        "reschedule",
        "relocate",
        "move_notebook",
    )

    fun validate(turn: StructuredTurn): StructuredTurn {
        when (turn.intent) {
            TurnIntent.Mutate -> {
                val action = turn.mutateAction?.trim()?.lowercase().orEmpty()
                if (action.isEmpty()) throw StructuredTurnValidationException("mutate 缺少 mutate_action")
                if (action !in mutateActions) {
                    throw StructuredTurnValidationException("不支持的 mutate_action")
                }
            }
            TurnIntent.Record -> {
                if (turn.entries.isEmpty()) {
                    throw StructuredTurnValidationException("record 没有条目")
                }
            }
            TurnIntent.Recall, TurnIntent.Clarify -> Unit
        }
        turn.entries.forEach { entry ->
            val type = entry.diaryType.trim().ifBlank { "life" }
            if (type !in diaryTypes) {
                throw StructuredTurnValidationException("不支持的 diary_type")
            }
        }
        return turn
    }
}
