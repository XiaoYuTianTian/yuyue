package com.yuyue.server.repo

import com.yuyue.server.db.NotebooksTable
import com.yuyue.server.db.UsersTable
import com.yuyue.server.util.CryptoUtil
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

object UserRepository {
    fun findByPhoneHash(hash: String): UUID? = transaction {
        UsersTable.selectAll().where { UsersTable.phoneHash eq hash }
            .firstOrNull()?.get(UsersTable.id)?.value
    }

    fun createUser(phone: String): UUID = transaction {
        val now = Instant.now()
        val hash = CryptoUtil.sha256(phone)
        val id = UUID.randomUUID()
        UsersTable.insert {
            it[UsersTable.id] = id
            it[phoneHash] = hash
            it[phoneMasked] = CryptoUtil.maskPhone(phone)
            it[timezone] = "Asia/Shanghai"
            it[createdAt] = now
            it[updatedAt] = now
        }
        NotebookRepository.seedDefaults(id)
        id
    }

    fun getOrCreate(phone: String): UUID {
        val hash = CryptoUtil.sha256(phone)
        return findByPhoneHash(hash) ?: createUser(phone)
    }
}
