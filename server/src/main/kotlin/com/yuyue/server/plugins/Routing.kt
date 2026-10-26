package com.yuyue.server.plugins

import com.yuyue.server.dto.*
import com.yuyue.server.repo.*
import com.yuyue.server.service.*
import com.yuyue.server.util.TimeUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

fun Application.configureRouting() {
    val json = Json { ignoreUnknownKeys = true }

    routing {
        get("/") {
            call.respond(
                ServiceInfoResponse(
                    app = "语曰 Yuyue",
                    message = "Backend API is running. Use /health for health check and /v1/* for API routes.",
                    health = "/health",
                )
            )
        }

        get("/health") {
            call.respond(HealthResponse(ok = true, app = "语曰 Yuyue"))
        }

        route("/v1/auth/sms") {
            post("/send") {
                val req = call.receive<SendSmsRequest>()
                val devCode = SmsService.sendOtp(req.phone)
                call.respond(SendSmsResponse("sent", if (com.yuyue.server.config.AppConfig.devMode) devCode else null))
            }
            post("/verify") {
                val req = call.receive<VerifySmsRequest>()
                if (!SmsService.verifyOtp(req.phone, req.code)) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("invalid code"))
                    return@post
                }
                val userId = UserRepository.getOrCreate(req.phone)
                call.respond(AuthResponse(JwtService.createToken(userId), userId.toString()))
            }
        }

        authenticate("auth-jwt") {
            route("/v1") {
                get("/notebooks") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    call.respond(NotebookRepository.listForUser(uid))
                }
                post("/notebooks") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val req = call.receive<CreateNotebookRequest>()
                    // simplified: skip insert for MVP - use seed only
                    call.respond(NotebookRepository.listForUser(uid))
                }

                get("/entries") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val notebookId = call.request.queryParameters["notebook_id"]?.let { UUID.fromString(it) }
                    val day = call.request.queryParameters["day"]?.let { LocalDate.parse(it) }
                    val zone = ZoneId.of("Asia/Shanghai")
                    val (from, to) = if (day != null) {
                        TimeUtil.dayRange(day, zone)
                    } else {
                        val f = call.request.queryParameters["from"]?.let { TimeUtil.parseInstant(it) }
                        val t = call.request.queryParameters["to"]?.let { TimeUtil.parseInstant(it) }
                        f to t
                    }
                    call.respond(
                        EntryRepository.listActive(uid, notebookId, from, to)
                    )
                }

                get("/entries/{id}") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    val entry = EntryRepository.get(id, uid)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("not found"))
                    call.respond(entry)
                }

                patch("/entries/{id}") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    val req = call.receive<PatchEntryRequest>()
                    val entry = EntryRepository.patch(id, uid, req)
                        ?: return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse("not found"))
                    call.respond(entry)
                }

                delete("/entries/{id}") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    if (EntryRepository.softDelete(id, uid)) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("not found"))
                    }
                }

                post("/entries/{id}/restore") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    if (EntryRepository.restore(id, uid)) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("not found"))
                    }
                }

                get("/trash") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    call.respond(EntryRepository.listTrash(uid))
                }

                post("/jobs/ingest") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    if (!QuotaService.checkAndIncrement(uid, "ingest")) {
                        call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("daily limit"))
                        return@post
                    }
                    val req = call.receive<IngestJobRequest>()
                    val jobId = JobRepository.create(uid, "ingest", json.encodeToString(req))
                    call.respond(HttpStatusCode.Accepted, JobDto(jobId.toString(), "ingest", "pending", null, null))
                }

                post("/jobs/recall") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    if (!QuotaService.checkAndIncrement(uid, "recall")) {
                        call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("daily limit"))
                        return@post
                    }
                    val req = call.receive<RecallJobRequest>()
                    val jobId = JobRepository.create(uid, "recall", json.encodeToString(req))
                    call.respond(HttpStatusCode.Accepted, JobDto(jobId.toString(), "recall", "pending"))
                }

                post("/jobs/mutate") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    if (!QuotaService.checkAndIncrement(uid, "mutate")) {
                        call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("daily limit"))
                        return@post
                    }
                    val req = call.receive<MutateJobRequest>()
                    val jobId = JobRepository.create(uid, "mutate", json.encodeToString(req))
                    call.respond(HttpStatusCode.Accepted, JobDto(jobId.toString(), "mutate", "pending"))
                }

                get("/jobs/{id}") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val jobId = UUID.fromString(call.parameters["id"])
                    val job = JobRepository.get(jobId, uid)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("not found"))
                    call.respond(job)
                }

                post("/ingest/confirm") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val body = call.receive<ConfirmIngestRequest>()
                    val jobId = UUID.fromString(body.jobId)
                    val job = JobRepository.get(jobId, uid)
                        ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("job not found"))
                    if (job.status != "succeeded") {
                        return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("job not ready"))
                    }
                    val outText = JobRepository.getRawOutput(jobId)
                        ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("no output"))
                    val saved = IngestConfirmService.confirmFromJobOutput(
                        uid,
                        UUID.fromString(body.notebookId),
                        outText,
                        body.idempotencyKey,
                    )
                    call.respond(saved)
                }

                post("/mutations/apply") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val req = call.receive<ApplyMutationsRequest>()
                    IngestConfirmService.applyMutations(uid, req.actions)
                    call.respond(mapOf("ok" to true))
                }

                get("/export") {
                    val uid = call.principal<JWTPrincipal>()!!.userId()
                    val notebooks = NotebookRepository.listForUser(uid)
                    val entries = EntryRepository.listActive(uid, null, null, null, includeCancelled = true)
                    call.respond(
                        ExportResponse(
                            exportedAt = TimeUtil.toIso(java.time.Instant.now()),
                            userId = uid.toString(),
                            notebooks = notebooks,
                            entries = entries,
                        )
                    )
                }
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class ServiceInfoResponse(
    val app: String,
    val message: String,
    val health: String,
)

@kotlinx.serialization.Serializable
data class HealthResponse(
    val ok: Boolean,
    val app: String,
)

@kotlinx.serialization.Serializable
data class ConfirmIngestRequest(
    val jobId: String,
    val notebookId: String,
    val idempotencyKey: String? = null,
)

private fun JWTPrincipal.userId(): UUID = UUID.fromString(payload.getClaim("sub").asString())
