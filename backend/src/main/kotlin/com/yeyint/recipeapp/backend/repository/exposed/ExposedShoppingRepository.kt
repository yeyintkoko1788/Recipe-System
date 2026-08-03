package com.yeyint.recipeapp.backend.repository.exposed

import com.yeyint.recipeapp.backend.config.dbQuery
import com.yeyint.recipeapp.backend.db.ShoppingItemsTable
import com.yeyint.recipeapp.backend.domain.ShoppingItem
import com.yeyint.recipeapp.backend.domain.ShoppingSource
import com.yeyint.recipeapp.backend.repository.ShoppingRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedShoppingRepository : ShoppingRepository {

    override suspend fun listForUser(userId: UUID, includePurchased: Boolean): List<ShoppingItem> = dbQuery {
        val query = ShoppingItemsTable.selectAll().where { ShoppingItemsTable.userId eq userId }
        if (!includePurchased) query.andWhere { ShoppingItemsTable.isPurchased eq false }
        query.orderBy(ShoppingItemsTable.createdAt, SortOrder.DESC).map { it.toItem() }
    }

    override suspend fun find(userId: UUID, id: UUID): ShoppingItem? = dbQuery {
        ShoppingItemsTable.selectAll().where {
            (ShoppingItemsTable.id eq id) and (ShoppingItemsTable.userId eq userId)
        }.singleOrNull()?.toItem()
    }

    override suspend fun add(
        userId: UUID, ingredientId: UUID?, name: String,
        quantity: BigDecimal?, unit: String?, source: ShoppingSource,
    ): ShoppingItem = dbQuery {
        val id = ShoppingItemsTable.insert {
            it[ShoppingItemsTable.userId] = userId
            it[ShoppingItemsTable.ingredientId] = ingredientId
            it[ShoppingItemsTable.name] = name
            it[ShoppingItemsTable.quantity] = quantity
            it[ShoppingItemsTable.unit] = unit
            it[ShoppingItemsTable.source] = source.name
            it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        } get ShoppingItemsTable.id
        ShoppingItemsTable.selectAll().where { ShoppingItemsTable.id eq id }.single().toItem()
    }

    override suspend fun existsUnpurchasedForIngredient(userId: UUID, ingredientId: UUID): Boolean = dbQuery {
        ShoppingItemsTable.selectAll().where {
            (ShoppingItemsTable.userId eq userId) and
                (ShoppingItemsTable.ingredientId eq ingredientId) and
                (ShoppingItemsTable.isPurchased eq false)
        }.limit(1).any()
    }

    override suspend fun markPurchased(userId: UUID, id: UUID): ShoppingItem? = dbQuery {
        val updated = ShoppingItemsTable.update({
            (ShoppingItemsTable.id eq id) and (ShoppingItemsTable.userId eq userId)
        }) {
            it[isPurchased] = true
            it[purchasedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) null
        else ShoppingItemsTable.selectAll().where { ShoppingItemsTable.id eq id }.single().toItem()
    }

    override suspend fun delete(userId: UUID, id: UUID): Boolean = dbQuery {
        ShoppingItemsTable.deleteWhere {
            (ShoppingItemsTable.id eq id) and (ShoppingItemsTable.userId eq userId)
        } > 0
    }

    override suspend fun clearPurchased(userId: UUID): Int = dbQuery {
        ShoppingItemsTable.deleteWhere {
            (ShoppingItemsTable.userId eq userId) and (ShoppingItemsTable.isPurchased eq true)
        }
    }

    private fun ResultRow.toItem() = ShoppingItem(
        id = this[ShoppingItemsTable.id].value,
        userId = this[ShoppingItemsTable.userId].value,
        ingredientId = this[ShoppingItemsTable.ingredientId]?.value,
        name = this[ShoppingItemsTable.name],
        quantity = this[ShoppingItemsTable.quantity],
        unit = this[ShoppingItemsTable.unit],
        source = ShoppingSource.valueOf(this[ShoppingItemsTable.source]),
        isPurchased = this[ShoppingItemsTable.isPurchased],
        createdAt = this[ShoppingItemsTable.createdAt].toInstant(),
        purchasedAt = this[ShoppingItemsTable.purchasedAt]?.toInstant(),
    )
}
