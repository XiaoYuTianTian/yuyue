package com.yuyue.server.db

import com.yuyue.server.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.nio.file.Paths

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = AppConfig.jdbcUrl
            username = AppConfig.jdbcUser
            password = AppConfig.jdbcPassword
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        Database.connect(HikariDataSource(config))
        runMigrations()
    }

    private fun runMigrations() {
        Files.createDirectories(Paths.get("data"))
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                NotebooksTable,
                EntriesTable,
                AsyncJobsTable,
                UserDailyUsageTable,
                SmsOtpTable,
            )
        }
    }
}
