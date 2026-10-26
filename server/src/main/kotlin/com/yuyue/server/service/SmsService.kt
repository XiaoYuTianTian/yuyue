package com.yuyue.server.service

import com.yuyue.server.config.AppConfig
import com.yuyue.server.db.SmsOtpTable
import com.yuyue.server.util.CryptoUtil
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import kotlin.random.Random

object SmsService {
    private val otpStore = mutableMapOf<String, Pair<String, Instant>>()

    fun sendOtp(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        require(digits.length >= 11) { "invalid phone" }
        val code = "%06d".format(Random.nextInt(0, 999999))
        val hash = CryptoUtil.sha256(digits)
        val codeHash = CryptoUtil.sha256(code)
        val expires = Instant.now().plusSeconds(300)

        transaction {
            SmsOtpTable.deleteWhere { SmsOtpTable.phoneHash eq hash }
            SmsOtpTable.insert {
                it[phoneHash] = hash
                it[SmsOtpTable.codeHash] = codeHash
                it[expiresAt] = expires
                it[attemptCount] = 0
                it[createdAt] = Instant.now()
            }
        }
        otpStore[hash] = code to expires
        if (AppConfig.devMode) {
            println("[Yuyue DEV] OTP for $digits: $code")
        }
        return if (AppConfig.devMode) code else ""
    }

    fun verifyOtp(phone: String, code: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        val hash = CryptoUtil.sha256(digits)
        val codeHash = CryptoUtil.sha256(code)
        return transaction {
            val row = SmsOtpTable.selectAll()
                .where { SmsOtpTable.phoneHash eq hash }
                .firstOrNull() ?: return@transaction false
            if (Instant.now().isAfter(row[SmsOtpTable.expiresAt])) return@transaction false
            if (row[SmsOtpTable.codeHash] != codeHash) {
                SmsOtpTable.update({ SmsOtpTable.phoneHash eq hash }) {
                    it[attemptCount] = row[SmsOtpTable.attemptCount] + 1
                }
                return@transaction false
            }
            SmsOtpTable.deleteWhere { SmsOtpTable.phoneHash eq hash }
            true
        }
    }
}
