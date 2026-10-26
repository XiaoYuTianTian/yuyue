package com.yuyue.server.service

import com.yuyue.server.dto.*
import com.yuyue.server.repo.*
import com.yuyue.server.util.TimeUtil
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

object JobWorker {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private var scope: CoroutineScope? = null

    fun start() {
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope?.launch {
            while (isActive) {
                val pair = JobRepository.claimPending()
                if (pair == null) {
                    delay(500)
                } else {
                    val (jobId, userId) = pair
                    runCatching { process(jobId, userId) }
                        .onFailure { JobRepository.fail(jobId, it.message ?: "unknown") }
                    delay(100)
                }
            }
        }
    }

    fun stop() {
        scope?.cancel()
    }

    private fun process(jobId: UUID, userId: UUID) {
        val job = JobRepository.get(jobId, userId) ?: return
        when (job.type) {
            "ingest" -> processIngest(jobId, userId, job)
            "recall" -> processRecall(jobId, userId, job)
            "mutate" -> processMutate(jobId, userId, job)
            else -> JobRepository.fail(jobId, "unknown job type")
        }
    }

    private fun processIngest(jobId: UUID, userId: UUID, job: JobDto) {
        val inputStr = JobRepository.getRawInput(jobId) ?: return
        val req = json.decodeFromString<IngestJobRequest>(inputStr)
        val notebooks = NotebookRepository.listForUser(userId)
        val nb = notebooks.find { it.id == req.notebookId }?.name ?: "生活"

        req.idempotencyKey?.let { key ->
            EntryRepository.findByIdempotency(userId, key)?.let { existing ->
                val out = buildJsonObject {
                    put("preview", true)
                    put("entries", Json.encodeToJsonElement(listOf(existing)))
                }
                JobRepository.complete(jobId, out.toString())
                return
            }
        }

        val mutatePlan = LlmService.parseMutate(req.rawTranscript, req.clientNow)
        if (mutatePlan != null) {
            val candidates = findCandidates(userId, mutatePlan.match)
            val out = buildJsonObject {
                put("intent", "mutate")
                put("matchPlan", json.encodeToJsonElement(mutatePlan))
                put("candidates", Json.encodeToJsonElement(candidates))
                put("requiresConfirmation", true)
            }
            JobRepository.complete(jobId, out.toString())
            return
        }

        val result = LlmService.parseIngest(req.rawTranscript, req.clientNow, nb)
        val previews = result.entries.map { e ->
            e.copy(statedAt = e.statedAt ?: req.clientNow)
        }
        val out = buildJsonObject {
            put("intent", "create")
            put("preview", true)
            put("entries", Json.encodeToJsonElement(previews))
        }
        JobRepository.complete(jobId, out.toString())
    }

    private fun processRecall(jobId: UUID, userId: UUID, job: JobDto) {
        val inputStr = JobRepository.getRawInput(jobId) ?: return
        val req = json.decodeFromString<RecallJobRequest>(inputStr)
        val notebooks = NotebookRepository.listForUser(userId)
        val plan = LlmService.buildQueryPlan(req.queryText, req.clientNow, notebooks)

        val notebookIds = plan.notebookIds?.map { UUID.fromString(it) }
            ?: notebooks.map { UUID.fromString(it.id) }
        val from = plan.timeRange?.startAt?.let { TimeUtil.parseInstant(it) }
        val to = plan.timeRange?.endAt?.let { TimeUtil.parseInstant(it) }
        val statuses = when (plan.temporalScope) {
            "as_of_now" -> listOf("active")
            "all_time" -> listOf("active", "superseded")
            else -> plan.statusFilter
        }

        var hits = EntryRepository.search(
            userId = userId,
            notebookIds = notebookIds,
            from = from,
            to = to,
            keywords = plan.keywords,
            subjectKeys = plan.subjectKeys,
            entryKinds = plan.entryKinds,
            statuses = statuses,
            includeSuperseded = plan.includeSuperseded,
            limit = plan.limit,
        )

        if (plan.temporalScope == "as_of_now" && plan.subjectKeys?.isNotEmpty() == true) {
            hits = hits.groupBy { it.subjectKey }
                .mapNotNull { (_, list) -> list.maxByOrNull { TimeUtil.parseInstant(it.statedAt) } }
        }

        val hitsJson = Json.encodeToString(hits)
        val answer = LlmService.summarizeRecall(req.queryText, hitsJson)
        val out = buildJsonObject {
            put("queryPlan", json.encodeToJsonElement(plan))
            put("hits", Json.encodeToJsonElement(hits))
            put("answer", answer)
            put("timeline", Json.encodeToJsonElement(groupTimeline(hits)))
        }
        JobRepository.complete(jobId, out.toString())
    }

    private fun processMutate(jobId: UUID, userId: UUID, job: JobDto) {
        val inputStr = JobRepository.getRawInput(jobId) ?: return
        val req = json.decodeFromString<MutateJobRequest>(inputStr)
        val plan = LlmService.parseMutate(req.rawTranscript, req.clientNow)
            ?: throw IllegalArgumentException("could not parse mutate intent")
        val candidates = findCandidates(userId, plan.match)
        val out = buildJsonObject {
            put("matchPlan", json.encodeToJsonElement(plan))
            put("candidates", Json.encodeToJsonElement(candidates))
            put("requiresConfirmation", true)
        }
        JobRepository.complete(jobId, out.toString())
    }

    private fun findCandidates(userId: UUID, match: LlmService.MatchCriteria): List<EntryDto> {
        val notebookIds = match.notebookIds?.map { UUID.fromString(it) }
        val from = match.timeRange?.startAt?.let { TimeUtil.parseInstant(it) }
        val to = match.timeRange?.endAt?.let { TimeUtil.parseInstant(it) }
        return EntryRepository.search(
            userId = userId,
            notebookIds = notebookIds,
            from = from,
            to = to,
            keywords = match.keywords,
            subjectKeys = null,
            entryKinds = match.entryKinds,
            statuses = listOf("active"),
            includeSuperseded = false,
            limit = 20,
        )
    }

    private fun groupTimeline(hits: List<EntryDto>): List<JsonObject> {
        return hits.groupBy { it.subjectKey ?: "general" }.map { (key, items) ->
            buildJsonObject {
                put("subjectKey", key)
                put("entries", Json.encodeToJsonElement(items.sortedBy { it.statedAt }))
            }
        }
    }
}
