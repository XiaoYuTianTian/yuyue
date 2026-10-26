package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Offline gold harness: LocalRouter + MultiEventSplit only.
 * Model transcripts are not accepted as PASS evidence (product contract).
 */
class Gold40HarnessTest {
    data class GoldCase(
        val id: String,
        val text: String,
        val expectIntent: String,
        val split: Int,
    )

    @Test
    fun fortyCasesMatchLocalL0Routing() {
        val cases = loadGold()
        assertEquals("gold40.jsonl must have 40 cases", 40, cases.size)

        val failures = mutableListOf<String>()
        for (c in cases) {
            val route = LocalRouter.classify(c.text)
            val expected = when (c.expectIntent.lowercase()) {
                "record" -> setOf(RouteKind.Record, RouteKind.Cloud)
                "recall" -> setOf(RouteKind.Recall)
                "mutate" -> setOf(RouteKind.Mutate)
                else -> emptySet()
            }
            if (route !in expected) {
                failures += "${c.id}: intent want=${c.expectIntent} got=$route text=${c.text}"
                continue
            }
            if (c.expectIntent.equals("record", ignoreCase = true) && c.split >= 2) {
                val parts = MultiEventSplit.entries(c.text, "2026-09-20T12:00:00+08:00")
                if (parts.size != c.split) {
                    failures += "${c.id}: split want=${c.split} got=${parts.size}"
                }
            }
            if (c.expectIntent.equals("recall", ignoreCase = true) ||
                c.expectIntent.equals("mutate", ignoreCase = true)
            ) {
                if (c.split != 0) failures += "${c.id}: non-record should declare split=0"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun threeRunsAreDeterministic() {
        val cases = loadGold()
        repeat(3) {
            for (c in cases) {
                val a = LocalRouter.classify(c.text)
                val b = LocalRouter.classify(c.text)
                assertEquals(c.id, a, b)
                if (c.split >= 2) {
                    val s1 = MultiEventSplit.entries(c.text, "2026-09-20T12:00:00+08:00").size
                    val s2 = MultiEventSplit.entries(c.text, "2026-09-20T12:00:00+08:00").size
                    assertEquals(c.id, s1, s2)
                }
            }
        }
    }

    private fun loadGold(): List<GoldCase> {
        val roots = listOf(
            File("docs/testing/gold40.jsonl"),
            File("../docs/testing/gold40.jsonl"),
            File("../../docs/testing/gold40.jsonl"),
            File("C:/File/Project/Yuyue/docs/testing/gold40.jsonl"),
        )
        val file = roots.firstOrNull { it.isFile }
            ?: error("gold40.jsonl not found from ${File(".").absolutePath}")
        return file.readLines()
            .filter { it.isNotBlank() }
            .map { line ->
                val id = Regex(""""id"\s*:\s*"([^"]+)"""").find(line)!!.groupValues[1]
                val text = Regex(""""text"\s*:\s*"([^"]+)"""").find(line)!!.groupValues[1]
                val intent = Regex(""""expect_intent"\s*:\s*"([^"]+)"""").find(line)!!.groupValues[1]
                val split = Regex(""""split"\s*:\s*(\d+)""").find(line)!!.groupValues[1].toInt()
                GoldCase(id, text, intent, split)
            }
    }
}
