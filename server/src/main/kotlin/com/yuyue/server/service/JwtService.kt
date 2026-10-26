package com.yuyue.server.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.yuyue.server.config.AppConfig
import java.util.Date
import java.util.UUID

object JwtService {
    private val algorithm = Algorithm.HMAC256(AppConfig.jwtSecret)
    private const val ISSUER = "yuyue"
    private const val EXPIRY_MS = 30L * 24 * 60 * 60 * 1000

    fun createToken(userId: UUID): String = JWT.create()
        .withIssuer(ISSUER)
        .withSubject(userId.toString())
        .withExpiresAt(Date(System.currentTimeMillis() + EXPIRY_MS))
        .sign(algorithm)

    fun verify(token: String): UUID? = runCatching {
        val verifier = JWT.require(algorithm).withIssuer(ISSUER).build()
        val sub = verifier.verify(token).subject
        UUID.fromString(sub)
    }.getOrNull()

    fun algorithm() = algorithm
    fun issuer() = ISSUER
}
