package com.yuyue.app.domain.migrate

/**
 * Privacy-safe aggregate counts for migration verification.
 * Never includes titles, bodies, or transcripts.
 */
data class DomainCountSnapshot(
    val notebooks: Int = 0,
    val entries: Int = 0,
    val captures: Int = 0,
    val media: Int = 0,
) {
    fun isEmpty(): Boolean =
        notebooks == 0 && entries == 0 && captures == 0 && media == 0

    fun hasData(): Boolean = !isEmpty()
}

data class DomainCountVerifyResult(
    val ok: Boolean,
    val expected: DomainCountSnapshot,
    val actual: DomainCountSnapshot,
    /** Counts-only detail; safe to log. */
    val message: String,
)

/**
 * Compares expected vs actual domain/legacy aggregate counts.
 */
object DomainCountVerifier {
    fun expectedFromImport(result: DomainImportResult): DomainCountSnapshot =
        DomainCountSnapshot(
            notebooks = result.notebooks.size,
            entries = result.events.size,
            captures = result.captures.size,
            media = result.mediaAssets.size,
        )

    /**
     * Legacy-side counts that the importer is expected to conserve for
     * notebooks / entries / media. Captures are derived after import mapping
     * (entries + incomplete ledger rows with content).
     */
    fun legacySourceCounts(snapshot: LegacyV7Snapshot): DomainCountSnapshot {
        val entryIds = snapshot.entries.map { it.id }.toSet()
        val incompleteCaptures = snapshot.incompleteLedger.count { ledger ->
            ledger.status != "Completed" &&
                (ledger.rawTranscript.isNotBlank() || !ledger.mediaJson.isNullOrBlank())
        }
        val media = snapshot.media.count { it.entryId in entryIds }
        return DomainCountSnapshot(
            notebooks = snapshot.notebooks.size,
            entries = snapshot.entries.size,
            captures = snapshot.entries.size + incompleteCaptures,
            media = media,
        )
    }

    fun verify(
        expected: DomainCountSnapshot,
        actual: DomainCountSnapshot,
    ): DomainCountVerifyResult {
        val mismatches = buildList {
            if (expected.notebooks != actual.notebooks) {
                add("notebooks expected=${expected.notebooks} actual=${actual.notebooks}")
            }
            if (expected.entries != actual.entries) {
                add("entries expected=${expected.entries} actual=${actual.entries}")
            }
            if (expected.captures != actual.captures) {
                add("captures expected=${expected.captures} actual=${actual.captures}")
            }
            if (expected.media != actual.media) {
                add("media expected=${expected.media} actual=${actual.media}")
            }
        }
        return if (mismatches.isEmpty()) {
            DomainCountVerifyResult(
                ok = true,
                expected = expected,
                actual = actual,
                message = "counts_ok notebooks=${actual.notebooks} entries=${actual.entries} " +
                    "captures=${actual.captures} media=${actual.media}",
            )
        } else {
            DomainCountVerifyResult(
                ok = false,
                expected = expected,
                actual = actual,
                message = "counts_mismatch " + mismatches.joinToString("; "),
            )
        }
    }
}
