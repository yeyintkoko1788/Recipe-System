package com.yeyint.recipeapp.backend.repository.exposed

import com.yeyint.recipeapp.backend.config.dbQuery
import com.yeyint.recipeapp.backend.db.RefreshTokensTable
import com.yeyint.recipeapp.backend.repository.RefreshTokenRepository
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedRefreshTokenRepository : RefreshTokenRepository {

    override suspend fun store(userId: UUID, tokenHash: String, expiresAt: Instant): Unit = dbQuery {
        RefreshTokensTable.insert {
            it[RefreshTokensTable.userId] = userId
            it[RefreshTokensTable.tokenHash] = tokenHash
            it[RefreshTokensTable.expiresAt] = expiresAt.atOffset(ZoneOffset.UTC)
            it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }

    override suspend fun findUserIdByValidToken(tokenHash: String, now: Instant): UUID? = dbQuery {
        RefreshTokensTable.selectAll()
            .where {
                (RefreshTokensTable.tokenHash eq tokenHash) and
                    (RefreshTokensTable.revoked eq false) and
                    (RefreshTokensTable.expiresAt greater now.atOffset(ZoneOffset.UTC))
            }
            .singleOrNull()
            ?.get(RefreshTokensTable.userId)?.value
    }

    override suspend fun revoke(tokenHash: String): Boolean = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.tokenHash eq tokenHash }) {
            it[revoked] = true
        } > 0
    }

    override suspend fun revokeAllForUser(userId: UUID): Unit = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.userId eq userId }) {
            it[revoked] = true
        }
    }
}
