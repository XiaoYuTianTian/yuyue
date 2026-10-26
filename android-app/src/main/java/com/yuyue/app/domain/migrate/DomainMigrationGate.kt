package com.yuyue.app.domain.migrate

/**
 * Decides whether first-launch / explicit migrate should run LegacyV7Importer.
 * Domain must be empty; legacy must have data. Never deletes legacy.
 */
object DomainMigrationGate {
    fun shouldImport(
        domain: DomainCountSnapshot,
        legacy: DomainCountSnapshot,
    ): Boolean = domain.isEmpty() && legacy.hasData()

    fun decision(
        domain: DomainCountSnapshot,
        legacy: DomainCountSnapshot,
    ): DomainMigrationDecision = when {
        !domain.isEmpty() -> DomainMigrationDecision.SkipDomainNotEmpty
        !legacy.hasData() -> DomainMigrationDecision.SkipLegacyEmpty
        else -> DomainMigrationDecision.RunImport
    }
}

enum class DomainMigrationDecision {
    RunImport,
    SkipDomainNotEmpty,
    SkipLegacyEmpty,
}
