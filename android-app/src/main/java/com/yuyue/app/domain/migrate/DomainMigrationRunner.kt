package com.yuyue.app.domain.migrate

import com.yuyue.app.domain.DomainRepository
import com.yuyue.app.observability.PrivacyLog

sealed class DomainMigrationOutcome {
    data object SkippedLegacyEmpty : DomainMigrationOutcome()
    data object SkippedDomainNotEmpty : DomainMigrationOutcome()
    data class Imported(
        val expected: DomainCountSnapshot,
        val actual: DomainCountSnapshot,
    ) : DomainMigrationOutcome()

    /** Soft failure: legacy untouched; mismatch logged with counts only. */
    data class CountMismatch(
        val expected: DomainCountSnapshot,
        val actual: DomainCountSnapshot,
        val detail: String,
    ) : DomainMigrationOutcome()

    data class Failed(val reason: String) : DomainMigrationOutcome()
}

/**
 * First-launch / explicit migrate: empty domain ← legacy via [LegacyV7Importer].
 * On count mismatch, fails soft (privacy-safe log) and does not delete legacy.
 */
class DomainMigrationRunner(
    private val loadLegacy: suspend () -> LegacyV7Snapshot,
    private val readDomainCounts: suspend () -> DomainCountSnapshot,
    private val persist: suspend (DomainImportResult) -> Unit,
    private val importer: (LegacyV7Snapshot) -> DomainImportResult = LegacyV7Importer::import,
    private val logEvent: (String, Map<String, String>) -> Unit = { name, extras ->
        runCatching { PrivacyLog.event(name, extras) }
    },
) {
    constructor(
        snapshotLoader: LegacyV7SnapshotLoader,
        domainRepository: DomainRepository,
        importer: (LegacyV7Snapshot) -> DomainImportResult = LegacyV7Importer::import,
    ) : this(
        loadLegacy = snapshotLoader::load,
        readDomainCounts = { domainRepository.counts() },
        persist = { domainRepository.persistImport(it) },
        importer = importer,
    )

    suspend fun runIfNeeded(): DomainMigrationOutcome {
        return try {
            val legacySnapshot = loadLegacy()
            val legacyCounts = DomainCountVerifier.legacySourceCounts(legacySnapshot)
            val domainCounts = readDomainCounts()
            when (DomainMigrationGate.decision(domainCounts, legacyCounts)) {
                DomainMigrationDecision.SkipLegacyEmpty -> DomainMigrationOutcome.SkippedLegacyEmpty
                DomainMigrationDecision.SkipDomainNotEmpty -> DomainMigrationOutcome.SkippedDomainNotEmpty
                DomainMigrationDecision.RunImport -> importAndVerify(legacySnapshot)
            }
        } catch (t: Throwable) {
            logEvent("domain_migrate_fail", mapOf("reason" to (t::class.simpleName ?: "error")))
            DomainMigrationOutcome.Failed(t::class.simpleName ?: "error")
        }
    }

    /** Explicit migrate entry point (same soft-fail rules). */
    suspend fun migrate(): DomainMigrationOutcome = runIfNeeded()

    private suspend fun importAndVerify(legacySnapshot: LegacyV7Snapshot): DomainMigrationOutcome {
        val imported = importer(legacySnapshot)
        persist(imported)
        val expected = DomainCountVerifier.expectedFromImport(imported)
        val actual = readDomainCounts()
        val verified = DomainCountVerifier.verify(expected, actual)
        val legacyExpected = DomainCountVerifier.legacySourceCounts(legacySnapshot)
        val conserved = DomainCountVerifier.verify(
            expected = legacyExpected.copy(captures = expected.captures),
            actual = actual,
        )
        return when {
            !verified.ok -> {
                logEvent("domain_migrate_mismatch", mapOf("detail" to verified.message))
                DomainMigrationOutcome.CountMismatch(expected, actual, verified.message)
            }
            !conserved.ok -> {
                logEvent("domain_migrate_mismatch", mapOf("detail" to conserved.message))
                DomainMigrationOutcome.CountMismatch(legacyExpected, actual, conserved.message)
            }
            else -> {
                logEvent(
                    "domain_migrate_ok",
                    mapOf(
                        "notebooks" to actual.notebooks.toString(),
                        "entries" to actual.entries.toString(),
                        "captures" to actual.captures.toString(),
                        "media" to actual.media.toString(),
                    ),
                )
                DomainMigrationOutcome.Imported(expected, actual)
            }
        }
    }
}
