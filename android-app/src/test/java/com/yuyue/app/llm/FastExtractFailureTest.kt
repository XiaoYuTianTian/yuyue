package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastExtractFailureTest {
    @Test
    fun clientHttpErrorIsSwallowed() {
        val err = IllegalStateException("Chat HTTP 402: {\"error\":\"insufficient credits\"}")
        assertTrue(FastExtractFailure.isClientHttpError(err))
        assertEquals("http=402", FastExtractFailure.logSafe(err))
    }

    @Test
    fun logSafeNeverIncludesBody() {
        val err = IllegalStateException("Chat HTTP 401: sk-secret-should-not-log")
        val logged = FastExtractFailure.logSafe(err)
        assertEquals("http=401", logged)
        assertFalse(logged.contains("sk-"))
    }

    @Test
    fun serverErrorIsNotTreatedAsEmptyExtract() {
        val err = IllegalStateException("Chat HTTP 500: oops")
        assertFalse(FastExtractFailure.isClientHttpError(err))
    }
}
