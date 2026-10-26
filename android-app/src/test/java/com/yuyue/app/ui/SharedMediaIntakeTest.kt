package com.yuyue.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedMediaIntakeTest {
    @Test
    fun textOnly_submitsWhenNotebookReady() {
        val r = SharedMediaIntake.decide(
            notebookId = "nb-1",
            text = "记下今天下午喝了咖啡",
            mimeType = "text/plain",
            hasStream = false,
        )
        assertTrue(r is SharedMediaIntake.SubmitText)
        assertEquals("记下今天下午喝了咖啡", (r as SharedMediaIntake.SubmitText).text)
    }

    @Test
    fun imageStream_submitsImageWhenNotebookReady() {
        val r = SharedMediaIntake.decide(
            notebookId = "nb-1",
            text = null,
            mimeType = "image/jpeg",
            hasStream = true,
        )
        assertTrue(r is SharedMediaIntake.SubmitImage)
    }

    @Test
    fun imagePlusCaption_holdsWhenNoNotebook() {
        val r = SharedMediaIntake.decide(
            notebookId = null,
            text = "今天的日落",
            mimeType = "image/*",
            hasStream = true,
        )
        assertTrue(r is SharedMediaIntake.HoldImage)
        assertEquals("今天的日落", (r as SharedMediaIntake.HoldImage).caption)
    }

    @Test
    fun emptyPayload_ignored() {
        assertTrue(
            SharedMediaIntake.decide(null, null, "text/plain", hasStream = false)
                is SharedMediaIntake.Ignore,
        )
        assertTrue(
            SharedMediaIntake.decide("nb-1", "  ", null, hasStream = false)
                is SharedMediaIntake.Ignore,
        )
    }

    @Test
    fun imageMimeDetected() {
        assertTrue(SharedMediaIntake.isImageMime("image/png"))
        assertTrue(SharedMediaIntake.isImageMime("image/*"))
        assertTrue(!SharedMediaIntake.isImageMime("text/plain"))
        assertTrue(!SharedMediaIntake.isImageMime(null))
    }
}
