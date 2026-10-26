package com.yuyue.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {
    @Test
    fun pinHashRoundTrip() {
        val salt = ByteArray(16) { 7 }
        val hash = AppLockCrypto.hashPin("1357", salt)
        assertTrue(AppLockCrypto.verifyPin("1357", salt, hash))
        assertFalse(AppLockCrypto.verifyPin("1358", salt, hash))
        assertFalse(AppLockCrypto.verifyPin("1357", ByteArray(16) { 8 }, hash))
    }

    @Test
    fun pinMustBeFourToEightDigits() {
        assertTrue(AppLockPolicy.isValidPin("1234"))
        assertTrue(AppLockPolicy.isValidPin("12345678"))
        assertFalse(AppLockPolicy.isValidPin("12"))
        assertFalse(AppLockPolicy.isValidPin("123456789"))
        assertFalse(AppLockPolicy.isValidPin("12ab"))
        assertFalse(AppLockPolicy.isValidPin(""))
    }

    @Test
    fun shouldChallengeWhenEnabledAndLocked() {
        assertTrue(AppLockPolicy.shouldChallenge(enabled = true, unlocked = false))
        assertFalse(AppLockPolicy.shouldChallenge(enabled = true, unlocked = true))
        assertFalse(AppLockPolicy.shouldChallenge(enabled = false, unlocked = false))
    }

    @Test
    fun biometricAllowedOnlyWhenEnabledAndHardwareOk() {
        assertTrue(AppLockPolicy.canUseBiometric(lockEnabled = true, biometricAvailable = true))
        assertFalse(AppLockPolicy.canUseBiometric(lockEnabled = true, biometricAvailable = false))
        assertFalse(AppLockPolicy.canUseBiometric(lockEnabled = false, biometricAvailable = true))
    }

    @Test
    fun unlockGraceResetsAfterBackground() {
        assertEquals(
            false,
            AppLockPolicy.stillUnlocked(
                wasUnlocked = true,
                backgroundedAtMillis = 1_000L,
                nowMillis = 1_000L + AppLockPolicy.GRACE_MILLIS + 1,
            ),
        )
        assertEquals(
            true,
            AppLockPolicy.stillUnlocked(
                wasUnlocked = true,
                backgroundedAtMillis = 1_000L,
                nowMillis = 1_000L + AppLockPolicy.GRACE_MILLIS - 1,
            ),
        )
    }
}
