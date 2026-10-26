package com.yuyue.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import com.yuyue.server.dto.ErrorResponse

fun Application.configureHTTP() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(cause.message ?: "error")
            )
        }
    }
}
