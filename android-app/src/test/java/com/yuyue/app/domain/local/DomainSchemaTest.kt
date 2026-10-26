package com.yuyue.app.domain.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager

class DomainSchemaTest {
    @Test
    fun versionOneCreatesIndependentDomainTables() {
        assertEquals(1, DomainSchema.VERSION)
        val sql = DomainSchema.createStatements().joinToString("\n")
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `domain_notebooks`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `captures`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `events`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `event_revisions`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `media_assets`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `topics`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `people`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `places`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `undo_commands`"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `domain_index_jobs`"))
        assertTrue(sql.contains("currentRevisionId"))
        assertTrue(sql.contains("rawText"))
        assertTrue(sql.contains("sha256"))
        assertTrue(sql.contains("relativePath"))
        assertTrue(!sql.contains("yuyue.db"))
    }

    @Test
    fun jdbcCreatesAllDomainTables() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
            conn.createStatement().use { st ->
                for (sql in DomainSchema.createStatements()) {
                    st.execute(sql)
                }
                val tables = st.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table'",
                ).use { rs ->
                    buildList {
                        while (rs.next()) add(rs.getString(1))
                    }
                }
                assertTrue(tables.contains("domain_notebooks"))
                assertTrue(tables.contains("captures"))
                assertTrue(tables.contains("events"))
                assertTrue(tables.contains("event_revisions"))
                assertTrue(tables.contains("media_assets"))
                assertTrue(tables.contains("undo_commands"))
                assertTrue(tables.contains("domain_index_jobs"))
            }
        }
    }

    @Test
    fun databaseNameIsSeparateFromLegacy() {
        assertEquals("yuyue-domain.db", YuyueDomainDatabase.NAME)
        assertEquals(1, YuyueDomainDatabase.VERSION)
    }
}
