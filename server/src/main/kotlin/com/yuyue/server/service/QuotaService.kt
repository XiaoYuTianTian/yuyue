package com.yuyue.server.service

import com.yuyue.server.config.AppConfig
import com.yuyue.server.db.UserDailyUsageTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID

object QuotaService {
    fun checkAndIncrement(userId: UUID, type: String): Boolean = transaction {
        val today = LocalDate.now()
        val row = UserDailyUsageTable.selectAll()
            .where { (UserDailyUsageTable.userId eq userId) and (UserDailyUsageTable.usageDate eq today) }
            .firstOrNull()

        val (ingest, recall) = if (row == null) {
            0 to 0
        } else {
            row[UserDailyUsageTable.ingestCount] to row[UserDailyUsageTable.recallCount]
        }

        val allowed = when (type) {
            "ingest", "mutate" -> ingest < AppConfig.dailyIngestLimit
            "recall" -> recall < AppConfig.dailyRecallLimit
            else -> true
        }
        if (!allowed) return@transaction false

        if (row == null) {
            UserDailyUsageTable.insert {
                it[UserDailyUsageTable.userId] = userId
                it[UserDailyUsageTable.usageDate] = today
                when (type) {
                    "ingest", "mutate" -> it[UserDailyUsageTable.ingestCount] = 1
                    "recall" -> it[UserDailyUsageTable.recallCount] = 1
                }
            }
        } else {
            UserDailyUsageTable.update({
                (UserDailyUsageTable.userId eq userId) and (UserDailyUsageTable.usageDate eq today)
            }) {
                when (type) {
                    "ingest", "mutate" -> it[UserDailyUsageTable.ingestCount] = ingest + 1
                    "recall" -> it[UserDailyUsageTable.recallCount] = recall + 1
                }
            }
        }
        true
    }
}
