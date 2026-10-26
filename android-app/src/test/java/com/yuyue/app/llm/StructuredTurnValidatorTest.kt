package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class StructuredTurnValidatorTest {
    @Test(expected = StructuredTurnParseException::class)
    fun rejectsUnknownIntentInsteadOfCoercing() {
        StructuredTurnParser.parse("""{"intent":"explode","entries":[]}""")
    }

    @Test(expected = StructuredTurnValidationException::class)
    fun mutateWithoutActionFails() {
        val turn = StructuredTurnParser.parse(
            """{"intent":"mutate","need_more":false,"entries":[]}""",
        )
        StructuredTurnValidator.validate(turn)
    }

    @Test
    fun validRecordPasses() {
        val turn = StructuredTurnParser.parse(
            """{"intent":"record","need_more":false,"time_explicit":false,
                "entries":[{"title":"散步","start_at":"2026-08-21T18:00:00+08:00",
                "place":"公园","people":[],"diary_type":"life","body":"晚饭后散步","raw":"晚饭后散步"}]}""",
        )
        val ok = StructuredTurnValidator.validate(turn)
        assertEquals(TurnIntent.Record, ok.intent)
        assertEquals("散步", ok.entries.single().title)
    }

    @Test(expected = StructuredTurnValidationException::class)
    fun unknownMutateActionFails() {
        val turn = StructuredTurnParser.parse(
            """{"intent":"mutate","mutate_action":"nuke","entries":[]}""",
        )
        StructuredTurnValidator.validate(turn)
    }
}
