package com.yeyint.recipeapp.backend.service

import com.yeyint.recipeapp.backend.domain.ShoppingItem
import com.yeyint.recipeapp.backend.domain.ShoppingSource
import com.yeyint.recipeapp.backend.dto.PurchaseRequest
import com.yeyint.recipeapp.backend.dto.ShoppingAddRequest
import com.yeyint.recipeapp.backend.repository.PantryRepository
import com.yeyint.recipeapp.backend.repository.ShoppingRepository
import com.yeyint.recipeapp.backend.util.AppException
import java.math.BigDecimal
import java.util.UUID

/**
 * Per-user shopping list.
 *
 * The list can be filled automatically from out-of-stock pantry items and,
 * when an item is purchased, the linked pantry entry is restocked — closing
 * the pantry → shopping → pantry loop.
 */
class ShoppingService(
    private val shopping: ShoppingRepository,
    private val pantry: PantryRepository,
) {

    suspend fun list(userId: UUID, includePurchased: Boolean): List<ShoppingItem> =
        shopping.listForUser(userId, includePurchased)

    /** Adds either a catalog-linked item (ingredientId) or a free-text item (name). */
    suspend fun add(userId: UUID, request: ShoppingAddRequest): ShoppingItem {
        val ingredientId = request.ingredientId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse { _ -> throw AppException.Validation(fields = mapOf("ingredientId" to "Must be a valid UUID")) }
        }
        return when {
            ingredientId != null -> {
                val pantryItem = pantry.findByIngredient(userId, ingredientId)
                val name = pantryItem?.ingredient?.name
                    ?: throw AppException.NotFound("Ingredient not found in your pantry; add it there first or use a free-text item")
                shopping.add(
                    userId, ingredientId, name,
                    request.quantity?.let(BigDecimal::valueOf),
                    request.unit ?: pantryItem.unit,
                    ShoppingSource.MANUAL,
                )
            }
            !request.name.isNullOrBlank() -> shopping.add(
                userId, null, request.name.trim(),
                request.quantity?.let(BigDecimal::valueOf), request.unit, ShoppingSource.MANUAL,
            )
            else -> throw AppException.Validation(
                fields = mapOf("name" to "Provide either an ingredientId or a name"),
            )
        }
    }

    /**
     * Creates shopping items for every out-of-stock pantry ingredient that is
     * not already on the (unpurchased) list. Returns the newly added items.
     */
    suspend fun generateFromPantry(userId: UUID): List<ShoppingItem> =
        pantry.outOfStockItems(userId)
            .filterNot { shopping.existsUnpurchasedForIngredient(userId, it.ingredient.id) }
            .map { item ->
                shopping.add(
                    userId, item.ingredient.id, item.ingredient.name,
                    quantity = null, unit = item.unit, source = ShoppingSource.PANTRY,
                )
            }

    /**
     * Marks an item purchased. If it is linked to a pantry ingredient and
     * [PurchaseRequest.restockPantry] is true, the pantry entry is restocked
     * with [PurchaseRequest.quantity] (or the item quantity, or 1 as fallback).
     */
    suspend fun purchase(userId: UUID, itemId: UUID, request: PurchaseRequest): ShoppingItem {
        val item = shopping.markPurchased(userId, itemId)
            ?: throw AppException.NotFound("Shopping item not found")

        if (request.restockPantry && item.ingredientId != null) {
            val pantryItem = pantry.findByIngredient(userId, item.ingredientId)
            if (pantryItem != null) {
                val restockQuantity = request.quantity
                    ?: item.quantity?.toDouble()
                    ?: 1.0
                pantry.update(
                    userId, pantryItem.id,
                    quantity = BigDecimal.valueOf(restockQuantity),
                    unit = null, isOutOfStock = false,
                )
            }
        }
        return item
    }

    suspend fun delete(userId: UUID, itemId: UUID) {
        if (!shopping.delete(userId, itemId)) throw AppException.NotFound("Shopping item not found")
    }

    suspend fun clearPurchased(userId: UUID): Int = shopping.clearPurchased(userId)
}
