package com.yuyue.server.util

import java.security.MessageDigest

object CryptoUtil {
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun maskPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 7) return "***"
        return digits.take(3) + "****" + digits.takeLast(4)
    }
}
