package com.yuyue.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentUndoCodecTest {
    @Test
    fun softDeleteRoundTrip() {
        val encoded = PersistentUndoCodec.encode(UndoAction.SoftDelete("e-42"))
        val decoded = PersistentUndoCodec.decode(encoded)
        assertTrue(decoded is UndoAction.SoftDelete)
        assertEquals("e-42", (decoded as UndoAction.SoftDelete).id)
    }

    @Test
    fun toggleDoneRoundTrip() {
        val encoded = PersistentUndoCodec.encode(UndoAction.ToggleDone("e-7", previous = true))
        val decoded = PersistentUndoCodec.decode(encoded) as UndoAction.ToggleDone
        assertEquals("e-7", decoded.id)
        assertEquals(true, decoded.previous)
    }

    @Test
    fun blankOrCorruptYieldsNull() {
        assertNull(PersistentUndoCodec.decode(null))
        assertNull(PersistentUndoCodec.decode(""))
        assertNull(PersistentUndoCodec.decode("{not-json"))
    }

    @Test
    fun expiredActionsAreDropped() {
        val encoded = PersistentUndoCodec.encode(
            UndoAction.SoftDelete("e-1"),
            createdAtMillis = 1_000L,
        )
        val ttl = PersistentUndoCodec.TTL_MILLIS
        assertNull(
            PersistentUndoCodec.decodeIfFresh(
                encoded,
                nowMillis = 1_000L + ttl + 1L,
            ),
        )
        assertTrue(
            PersistentUndoCodec.decodeIfFresh(
                encoded,
                nowMillis = 1_000L + ttl - 1L,
            ) is UndoAction.SoftDelete,
        )
    }
}
