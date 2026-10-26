package com.yuyue.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.yuyue.server.service.JwtService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(JwtService.algorithm())
                    .withIssuer(JwtService.issuer())
                    .build()
            )
            validate { credential ->
                val sub = credential.payload.subject ?: return@validate null
                JWTPrincipal(credential.payload)
            }
        }
    }
}
