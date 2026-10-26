package com.yuyue.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppLockCrypto {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun verifyPin(pin: String, salt: ByteArray, expected: ByteArray): Boolean {
        val actual = hashPin(pin, salt)
        return MessageDigest.isEqual(actual, expected)
    }
}

object AppLockPolicy {
    const val GRACE_MILLIS: Long = 30_000L

    fun isValidPin(pin: String): Boolean = pin.length in 4..8 && pin.all { it.isDigit() }

    fun shouldChallenge(enabled: Boolean, unlocked: Boolean): Boolean = enabled && !unlocked

    fun canUseBiometric(lockEnabled: Boolean, biometricAvailable: Boolean): Boolean =
        lockEnabled && biometricAvailable

    fun stillUnlocked(wasUnlocked: Boolean, backgroundedAtMillis: Long?, nowMillis: Long): Boolean {
        if (!wasUnlocked) return false
        val at = backgroundedAtMillis ?: return true
        return nowMillis - at <= GRACE_MILLIS
    }
}
