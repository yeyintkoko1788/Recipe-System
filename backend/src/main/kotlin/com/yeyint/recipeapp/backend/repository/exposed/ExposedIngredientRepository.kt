package com.yeyint.recipeapp.backend.repository.exposed

import com.yeyint.recipeapp.backend.config.dbQuery
import com.yeyint.recipeapp.backend.db.IngredientsTable
import com.yeyint.recipeapp.backend.domain.Ingredient
import com.yeyint.recipeapp.backend.domain.IngredientStatus
import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.repository.IngredientRepository
import com.yeyint.recipeapp.backend.util.PageRequest
import com.yeyint.recipeapp.backend.util.SortDirection
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedIngredientRepository : IngredientRepository {

    override suspend fun findById(id: UUID): Ingredient? = dbQuery {
        IngredientsTable.selectAll().where { IngredientsTable.id eq id }.singleOrNull()?.toIngredient()
    }

    override suspend fun findByName(name: String): Ingredient? = dbQuery {
        IngredientsTable.selectAll()
            .where { IngredientsTable.name.lowerCase() eq name.trim().lowercase() }
            .singleOrNull()?.toIngredient()
    }

    override suspend fun existingIds(ids: Collection<UUID>): Set<UUID> = dbQuery {
        if (ids.isEmpty()) emptySet()
        else IngredientsTable.selectAll()
            .where { IngredientsTable.id inList ids }
            .map { it[IngredientsTable.id].value }
            .toSet()
    }

    override suspend fun create(
        name: String, category: String, defaultUnit: String,
        imageUrl: String?, status: IngredientStatus, createdBy: UUID?,
    ): Ingredient = dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val id = IngredientsTable.insert {
            it[IngredientsTable.name] = name.trim()
            it[IngredientsTable.category] = category
            it[IngredientsTable.defaultUnit] = defaultUnit
            it[IngredientsTable.imageUrl] = imageUrl
            it[IngredientsTable.status] = status.name
            it[IngredientsTable.createdBy] = createdBy
            it[createdAt] = now
            it[updatedAt] = now
        } get IngredientsTable.id
        IngredientsTable.selectAll().where { IngredientsTable.id eq id }.single().toIngredient()
    }

    override suspend fun update(
        id: UUID, name: String, category: String, defaultUnit: String, imageUrl: String?,
    ): Ingredient? = dbQuery {
        val updated = IngredientsTable.update({ IngredientsTable.id eq id }) {
            it[IngredientsTable.name] = name.trim()
            it[IngredientsTable.category] = category
            it[IngredientsTable.defaultUnit] = defaultUnit
            it[IngredientsTable.imageUrl] = imageUrl
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) null
        else IngredientsTable.selectAll().where { IngredientsTable.id eq id }.single().toIngredient()
    }

    override suspend fun updateStatus(id: UUID, status: IngredientStatus): Boolean = dbQuery {
        IngredientsTable.update({ IngredientsTable.id eq id }) {
            it[IngredientsTable.status] = status.name
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        } > 0
    }

    override suspend fun delete(id: UUID): Boolean = dbQuery {
        IngredientsTable.deleteWhere { IngredientsTable.id eq id } > 0
    }

    override suspend fun list(
        page: PageRequest, search: String?, category: String?, status: IngredientStatus?,
    ): Page<Ingredient> = dbQuery {
        val query = IngredientsTable.selectAll()
        search?.takeIf { it.isNotBlank() }?.let { q ->
            query.andWhere { IngredientsTable.name.lowerCase() like "%${q.lowercase()}%" }
        }
        category?.let { query.andWhere { IngredientsTable.category eq it } }
        status?.let { query.andWhere { IngredientsTable.status eq it.name } }

        val total = query.count()
        val sortColumn = when (page.sortBy) {
            "category" -> IngredientsTable.category
            "createdAt" -> IngredientsTable.createdAt
            else -> IngredientsTable.name
        }
        val items = query
            .orderBy(sortColumn, if (page.direction == SortDirection.ASC) SortOrder.ASC else SortOrder.DESC)
            .limit(page.size).offset(page.offset)
            .map { it.toIngredient() }
        Page(items, page.page, page.size, total)
    }

    private fun ResultRow.toIngredient() = Ingredient(
        id = this[IngredientsTable.id].value,
        name = this[IngredientsTable.name],
        category = this[IngredientsTable.category],
        defaultUnit = this[IngredientsTable.defaultUnit],
        imageUrl = this[IngredientsTable.imageUrl],
        status = IngredientStatus.valueOf(this[IngredientsTable.status]),
        createdBy = this[IngredientsTable.createdBy]?.value,
        createdAt = this[IngredientsTable.createdAt].toInstant(),
        updatedAt = this[IngredientsTable.updatedAt].toInstant(),
    )
}
