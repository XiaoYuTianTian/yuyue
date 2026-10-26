package com.yuyue.app.domain.migrate

import com.yuyue.app.data.local.YuyueDatabase

/** Loads a [LegacyV7Snapshot] from the live v7 Room database (QA/code paths only). */
class LegacyV7SnapshotLoader(
    private val legacy: YuyueDatabase,
) {
    suspend fun load(): LegacyV7Snapshot = LegacyV7Snapshot(
        notebooks = legacy.notebookDao().listAll(),
        entries = legacy.entryDao().listAll(),
        revisions = legacy.entryRevisionDao().listAll(),
        media = legacy.entryMediaDao().listAll(),
        incompleteLedger = legacy.captureLedgerDao().listIncomplete(),
        preferenceSnapshot = emptyMap(),
        mediaSha256ById = emptyMap(),
    )

    suspend fun legacyCounts(): DomainCountSnapshot {
        val snapshot = load()
        return DomainCountVerifier.legacySourceCounts(snapshot)
    }
}
