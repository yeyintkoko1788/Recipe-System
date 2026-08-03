package com.yeyint.recipeapp.backend.service

import com.yeyint.recipeapp.backend.domain.PantryItem
import com.yeyint.recipeapp.backend.dto.PantryUpdateRequest
import com.yeyint.recipeapp.backend.dto.PantryUpsertRequest
import com.yeyint.recipeapp.backend.repository.IngredientRepository
import com.yeyint.recipeapp.backend.repository.PantryRepository
import com.yeyint.recipeapp.backend.util.AppException
import com.yeyint.recipeapp.backend.util.validate
import java.math.BigDecimal
import java.util.UUID

/** Per-user pantry management. */
class PantryService(
    private val pantry: PantryRepository,
    private val ingredients: IngredientRepository,
) {

    suspend fun list(userId: UUID): List<PantryItem> = pantry.listForUser(userId)

    /** Adds an ingredient to the pantry, or replaces quantity/unit if present. */
    suspend fun upsert(userId: UUID, request: PantryUpsertRequest): PantryItem {
        validate {
            require(request.quantity >= 0, "quantity", "Quantity must not be negative")
        }
        val ingredientId = parseUuid(request.ingredientId, "ingredientId")
        val ingredient = ingredients.findById(ingredientId)
            ?: throw AppException.NotFound("Ingredient not found")
        val unit = request.unit?.takeIf { it.isNotBlank() } ?: ingredient.defaultUnit
        return pantry.upsert(userId, ingredientId, BigDecimal.valueOf(request.quantity), unit)
    }

    suspend fun update(userId: UUID, itemId: UUID, request: PantryUpdateRequest): PantryItem {
        validate {
            request.quantity?.let { require(it >= 0, "quantity", "Quantity must not be negative") }
        }
        // Setting quantity to zero implies out-of-stock unless explicitly overridden.
        val outOfStock = request.isOutOfStock ?: request.quantity?.let { it == 0.0 }
        return pantry.update(
            userId, itemId,
            quantity = request.quantity?.let(BigDecimal::valueOf),
            unit = request.unit,
            isOutOfStock = outOfStock,
        ) ?: throw AppException.NotFound("Pantry item not found")
    }

    /** Marks the item empty — it becomes a candidate for the shopping list. */
    suspend fun markOutOfStock(userId: UUID, itemId: UUID): PantryItem =
        pantry.update(userId, itemId, quantity = BigDecimal.ZERO, unit = null, isOutOfStock = true)
            ?: throw AppException.NotFound("Pantry item not found")

    /** Restores stock after shopping. */
    suspend fun restock(userId: UUID, itemId: UUID, quantity: Double): PantryItem {
        validate { require(quantity > 0, "quantity", "Quantity must be greater than 0") }
        return pantry.update(
            userId, itemId,
            quantity = BigDecimal.valueOf(quantity), unit = null, isOutOfStock = false,
        ) ?: throw AppException.NotFound("Pantry item not found")
    }

    suspend fun delete(userId: UUID, itemId: UUID) {
        if (!pantry.delete(userId, itemId)) throw AppException.NotFound("Pantry item not found")
    }

    private fun parseUuid(raw: String, field: String): UUID =
        runCatching { UUID.fromString(raw) }
            .getOrElse { throw AppException.Validation(fields = mapOf(field to "Must be a valid UUID")) }
}
