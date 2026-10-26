package com.yuyue.app.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TurnIntent {
    @SerialName("record")
    Record,

    @SerialName("recall")
    Recall,

    @SerialName("clarify")
    Clarify,

    @SerialName("mutate")
    Mutate,
}

@Serializable
data class StructuredEntry(
    val title: String,
    @SerialName("start_at") val startAt: String,
    val place: String = "",
    val people: List<String> = emptyList(),
    @SerialName("diary_type") val diaryType: String = "life",
    val body: String = "",
    val raw: String = "",
)

@Serializable
data class RecallQuery(
    @SerialName("time_from") val timeFrom: String? = null,
    @SerialName("time_to") val timeTo: String? = null,
    @SerialName("diary_type") val diaryType: String? = null,
    val keyword: String? = null,
    @SerialName("title_hint") val titleHint: String? = null,
) {
    fun isEmpty(): Boolean =
        timeFrom.isNullOrBlank() &&
            timeTo.isNullOrBlank() &&
            diaryType.isNullOrBlank() &&
            keyword.isNullOrBlank() &&
            titleHint.isNullOrBlank()
}

@Serializable
data class StructuredTurn(
    val intent: TurnIntent,
    @SerialName("need_more") val needMore: Boolean = false,
    @SerialName("clarify_question") val clarifyQuestion: String? = null,
    val entries: List<StructuredEntry> = emptyList(),
    @SerialName("recall_query") val recallQuery: RecallQuery? = null,
    @SerialName("time_explicit") val timeExplicit: Boolean = false,
    @SerialName("missing_slots") val missingSlots: List<String> = emptyList(),
    @SerialName("mutate_action") val mutateAction: String? = null,
    @SerialName("target_hint") val targetHint: RecallQuery? = null,
)

class StructuredTurnParseException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
