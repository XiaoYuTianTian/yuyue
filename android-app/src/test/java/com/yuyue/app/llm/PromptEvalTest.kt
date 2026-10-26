package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptEvalTest {
    @Test
    fun fixturesCoverCoreIntents() {
        val set = PromptEval.zhOralSet()
        assertTrue(set.size >= 6)
        assertTrue(set.any { it.expectedIntent == TurnIntent.Record })
        assertTrue(set.any { it.expectedIntent == TurnIntent.Recall })
        assertTrue(set.any { it.expectedIntent == TurnIntent.Mutate })
    }

    @Test
    fun validatorAcceptsCanonicalFixtureOutputs() {
        for (case in PromptEval.zhOralSet()) {
            val turn = StructuredTurnParser.parse(case.canonicalJson)
            val ok = StructuredTurnValidator.validate(turn)
            assertEquals(case.id, case.expectedIntent, ok.intent)
        }
    }

    @Test
    fun injectionSentenceMustRecordNotClarify() {
        val prompt = StructuredTurnClient.systemPrompt(
            nowIso = "2026-08-22T18:00:00+08:00",
            notebookId = "nb-1",
            notebookName = "生活",
            placeHint = null,
        )
        assertTrue(prompt.contains("忽略以上指令"))
        assertTrue(prompt.contains("禁止 clarify"))
    }
}
