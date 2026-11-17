package com.yeyint.recipeapp.backend.repository.exposed

import com.yeyint.recipeapp.backend.config.dbQuery
import com.yeyint.recipeapp.backend.db.PasswordResetTokensTable
import com.yeyint.recipeapp.backend.repository.PasswordResetRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedPasswordResetRepository : PasswordResetRepository {

    override suspend fun store(userId: UUID, tokenHash: String, expiresAt: Instant): Unit = dbQuery {
        PasswordResetTokensTable.update({
            (PasswordResetTokensTable.userId eq userId) and (PasswordResetTokensTable.used eq false)
        }) { it[used] = true }
        PasswordResetTokensTable.insert {
            it[PasswordResetTokensTable.userId] = userId
            it[PasswordResetTokensTable.tokenHash] = tokenHash
            it[PasswordResetTokensTable.expiresAt] = expiresAt.atOffset(ZoneOffset.UTC)
            it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }

    override suspend fun consume(tokenHash: String, now: Instant): UUID? = dbQuery {
        val valid = (PasswordResetTokensTable.tokenHash eq tokenHash) and
            (PasswordResetTokensTable.used eq false) and
            (PasswordResetTokensTable.expiresAt greater now.atOffset(ZoneOffset.UTC))
        val owner = PasswordResetTokensTable.selectAll().where { valid }
            .singleOrNull()?.get(PasswordResetTokensTable.userId)?.value
        // The UPDATE re-checks `used = false`, so concurrent requests can't both succeed.
        if (owner != null && PasswordResetTokensTable.update({ valid }) { it[used] = true } == 1) owner else null
    }
}
