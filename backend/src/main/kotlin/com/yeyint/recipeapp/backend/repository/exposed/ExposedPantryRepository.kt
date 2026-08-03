package com.yeyint.recipeapp.backend.repository.exposed

import com.yeyint.recipeapp.backend.config.dbQuery
import com.yeyint.recipeapp.backend.db.IngredientsTable
import com.yeyint.recipeapp.backend.db.PantryItemsTable
import com.yeyint.recipeapp.backend.domain.Ingredient
import com.yeyint.recipeapp.backend.domain.IngredientStatus
import com.yeyint.recipeapp.backend.domain.PantryItem
import com.yeyint.recipeapp.backend.repository.PantryRepository
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedPantryRepository : PantryRepository {

    override suspend fun listForUser(userId: UUID): List<PantryItem> = dbQuery {
        joined().where { PantryItemsTable.userId eq userId }
            .orderBy(IngredientsTable.name, SortOrder.ASC)
            .map { it.toPantryItem() }
    }

    override suspend fun find(userId: UUID, id: UUID): PantryItem? = dbQuery {
        joined().where { (PantryItemsTable.id eq id) and (PantryItemsTable.userId eq userId) }
            .singleOrNull()?.toPantryItem()
    }

    override suspend fun findByIngredient(userId: UUID, ingredientId: UUID): PantryItem? = dbQuery {
        joined().where {
            (PantryItemsTable.userId eq userId) and (PantryItemsTable.ingredientId eq ingredientId)
        }.singleOrNull()?.toPantryItem()
    }

    override suspend fun upsert(
        userId: UUID, ingredientId: UUID, quantity: BigDecimal, unit: String,
    ): PantryItem = dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val existing = PantryItemsTable.selectAll().where {
            (PantryItemsTable.userId eq userId) and (PantryItemsTable.ingredientId eq ingredientId)
        }.singleOrNull()

        val id = if (existing == null) {
            (PantryItemsTable.insert {
                it[PantryItemsTable.userId] = userId
                it[PantryItemsTable.ingredientId] = ingredientId
                it[PantryItemsTable.quantity] = quantity
                it[PantryItemsTable.unit] = unit
                it[isOutOfStock] = quantity.signum() == 0
                it[updatedAt] = now
            } get PantryItemsTable.id).value
        } else {
            val existingId = existing[PantryItemsTable.id].value
            PantryItemsTable.update({ PantryItemsTable.id eq existingId }) {
                it[PantryItemsTable.quantity] = quantity
                it[PantryItemsTable.unit] = unit
                it[isOutOfStock] = quantity.signum() == 0
                it[updatedAt] = now
            }
            existingId
        }
        joined().where { PantryItemsTable.id eq id }.single().toPantryItem()
    }

    override suspend fun update(
        userId: UUID, id: UUID, quantity: BigDecimal?, unit: String?, isOutOfStock: Boolean?,
    ): PantryItem? = dbQuery {
        val updated = PantryItemsTable.update({
            (PantryItemsTable.id eq id) and (PantryItemsTable.userId eq userId)
        }) { stmt ->
            quantity?.let { stmt[PantryItemsTable.quantity] = it }
            unit?.let { stmt[PantryItemsTable.unit] = it }
            isOutOfStock?.let { stmt[PantryItemsTable.isOutOfStock] = it }
            stmt[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) null
        else joined().where { PantryItemsTable.id eq id }.single().toPantryItem()
    }

    override suspend fun delete(userId: UUID, id: UUID): Boolean = dbQuery {
        PantryItemsTable.deleteWhere {
            (PantryItemsTable.id eq id) and (PantryItemsTable.userId eq userId)
        } > 0
    }

    override suspend fun outOfStockItems(userId: UUID): List<PantryItem> = dbQuery {
        joined().where {
            (PantryItemsTable.userId eq userId) and (PantryItemsTable.isOutOfStock eq true)
        }.map { it.toPantryItem() }
    }

    private fun joined() =
        PantryItemsTable.join(IngredientsTable, JoinType.INNER, PantryItemsTable.ingredientId, IngredientsTable.id)
            .selectAll()

    private fun ResultRow.toPantryItem() = PantryItem(
        id = this[PantryItemsTable.id].value,
        userId = this[PantryItemsTable.userId].value,
        ingredient = Ingredient(
            id = this[IngredientsTable.id].value,
            name = this[IngredientsTable.name],
            category = this[IngredientsTable.category],
            defaultUnit = this[IngredientsTable.defaultUnit],
            imageUrl = this[IngredientsTable.imageUrl],
            status = IngredientStatus.valueOf(this[IngredientsTable.status]),
            createdBy = this[IngredientsTable.createdBy]?.value,
            createdAt = this[IngredientsTable.createdAt].toInstant(),
            updatedAt = this[IngredientsTable.updatedAt].toInstant(),
        ),
        quantity = this[PantryItemsTable.quantity],
        unit = this[PantryItemsTable.unit],
        isOutOfStock = this[PantryItemsTable.isOutOfStock],
        updatedAt = this[PantryItemsTable.updatedAt].toInstant(),
    )
}
