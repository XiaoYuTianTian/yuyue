package com.yuyue.server.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class SendSmsRequest(val phone: String)

@Serializable
data class SendSmsResponse(val message: String, val devCode: String? = null)

@Serializable
data class VerifySmsRequest(val phone: String, val code: String)

@Serializable
data class AuthResponse(val token: String, val userId: String)

@Serializable
data class NotebookDto(
    val id: String,
    val name: String,
    val slug: String,
    val color: String? = null,
    val icon: String? = null,
    val sortOrder: Int,
    val isDefault: Boolean,
)

@Serializable
data class CreateNotebookRequest(val name: String, val slug: String, val color: String? = null)

@Serializable
data class EntryDto(
    val id: String,
    val notebookId: String,
    val entryKind: String,
    val status: String,
    val subjectKey: String? = null,
    val polarity: String? = null,
    val supersedesEntryId: String? = null,
    val title: String,
    val body: String,
    val rawTranscript: String? = null,
    val startAt: String,
    val endAt: String? = null,
    val statedAt: String,
    val confidence: Float? = null,
    val sourceQuote: String? = null,
    val updatedAt: String,
)

@Serializable
data class PatchEntryRequest(
    val title: String? = null,
    val body: String? = null,
    val notebookId: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val status: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class IngestJobRequest(
    val notebookId: String,
    val rawTranscript: String,
    val clientNow: String,
    val locale: String = "zh-CN",
    val idempotencyKey: String? = null,
)

@Serializable
data class RecallJobRequest(
    val queryText: String,
    val clientNow: String,
    val notebookId: String? = null,
    val locale: String = "zh-CN",
)

@Serializable
data class MutateJobRequest(
    val rawTranscript: String,
    val clientNow: String,
    val notebookId: String? = null,
    val locale: String = "zh-CN",
)

@Serializable
data class JobDto(
    val id: String,
    val type: String,
    val status: String,
    val output: JsonElement? = null,
    val error: String? = null,
)

@Serializable
data class ApplyMutationsRequest(
    val jobId: String,
    val actions: List<MutationAction>,
)

@Serializable
data class MutationAction(
    val entryId: String,
    val action: String,
)

@Serializable
data class ExportResponse(
    val exportVersion: Int = 1,
    val exportedAt: String,
    val userId: String,
    val notebooks: List<NotebookDto>,
    val entries: List<EntryDto>,
)
