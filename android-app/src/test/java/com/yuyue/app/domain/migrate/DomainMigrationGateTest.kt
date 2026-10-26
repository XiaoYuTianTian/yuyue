package com.yuyue.app.domain.migrate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMigrationGateTest {
    @Test
    fun runsWhenDomainEmptyAndLegacyHasData() {
        val domain = DomainCountSnapshot()
        val legacy = DomainCountSnapshot(notebooks = 1, entries = 2, captures = 2, media = 0)
        assertTrue(DomainMigrationGate.shouldImport(domain, legacy))
        assertEquals(DomainMigrationDecision.RunImport, DomainMigrationGate.decision(domain, legacy))
    }

    @Test
    fun skipsWhenDomainAlreadyHasRows() {
        val domain = DomainCountSnapshot(entries = 1)
        val legacy = DomainCountSnapshot(entries = 5)
        assertFalse(DomainMigrationGate.shouldImport(domain, legacy))
        assertEquals(
            DomainMigrationDecision.SkipDomainNotEmpty,
            DomainMigrationGate.decision(domain, legacy),
        )
    }

    @Test
    fun skipsWhenLegacyEmpty() {
        val domain = DomainCountSnapshot()
        val legacy = DomainCountSnapshot()
        assertFalse(DomainMigrationGate.shouldImport(domain, legacy))
        assertEquals(
            DomainMigrationDecision.SkipLegacyEmpty,
            DomainMigrationGate.decision(domain, legacy),
        )
    }

    @Test
    fun legacyMediaAloneTriggersImport() {
        val domain = DomainCountSnapshot()
        val legacy = DomainCountSnapshot(media = 3)
        assertTrue(DomainMigrationGate.shouldImport(domain, legacy))
    }
}
