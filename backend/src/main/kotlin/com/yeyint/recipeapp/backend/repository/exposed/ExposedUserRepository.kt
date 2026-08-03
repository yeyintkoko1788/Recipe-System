package com.yeyint.recipeapp.backend.repository.exposed

import com.yeyint.recipeapp.backend.config.dbQuery
import com.yeyint.recipeapp.backend.db.UsersTable
import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.User
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.repository.UserRepository
import com.yeyint.recipeapp.backend.util.PageRequest
import com.yeyint.recipeapp.backend.util.SortDirection
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedUserRepository : UserRepository {

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email.lowerCase() eq email.lowercase() }
            .singleOrNull()?.toUser()
    }

    override suspend fun findById(id: UUID): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
    }

    override suspend fun create(name: String, email: String, passwordHash: String, role: UserRole): User = dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = UsersTable.insert {
            it[UsersTable.name] = name
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role.name
            it[createdAt] = now
            it[updatedAt] = now
        } get UsersTable.id
        UsersTable.selectAll().where { UsersTable.id eq id }.single().toUser()
    }

    override suspend fun list(page: PageRequest, search: String?): Page<User> = dbQuery {
        val query = UsersTable.selectAll()
        search?.takeIf { it.isNotBlank() }?.let { q ->
            val term = "%${q.lowercase()}%"
            query.andWhere {
                (UsersTable.name.lowerCase() like term) or (UsersTable.email.lowerCase() like term)
            }
        }
        val total = query.count()
        val sortColumn = when (page.sortBy) {
            "name" -> UsersTable.name
            "email" -> UsersTable.email
            else -> UsersTable.createdAt
        }
        val items = query
            .orderBy(sortColumn, if (page.direction == SortDirection.ASC) SortOrder.ASC else SortOrder.DESC)
            .limit(page.size).offset(page.offset)
            .map { it.toUser() }
        Page(items, page.page, page.size, total)
    }

    override suspend fun updateRole(id: UUID, role: UserRole): Boolean = dbQuery {
        UsersTable.update({ UsersTable.id eq id }) {
            it[UsersTable.role] = role.name
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        } > 0
    }

    override suspend fun delete(id: UUID): Boolean = dbQuery {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id].value,
        name = this[UsersTable.name],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        role = UserRole.valueOf(this[UsersTable.role]),
        createdAt = this[UsersTable.createdAt].toInstant(),
        updatedAt = this[UsersTable.updatedAt].toInstant(),
    )
}
