package com.yeyint.recipeapp.backend.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class UserRole { ADMIN, USER }
enum class Difficulty { EASY, MEDIUM, HARD }
enum class IngredientStatus { APPROVED, PENDING, REJECTED }
enum class ShoppingSource { MANUAL, PANTRY }

/** A registered account. [passwordHash] never leaves the backend. */
data class User(
    val id: UUID,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class Ingredient(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultUnit: String,
    val imageUrl: String?,
    val status: IngredientStatus,
    val createdBy: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** One ingredient line inside a recipe (join row + catalog name). */
data class RecipeIngredient(
    val ingredientId: UUID,
    val ingredientName: String,
    val quantity: BigDecimal,
    val unit: String,
    val note: String?,
)

data class Recipe(
    val id: UUID,
    val title: String,
    val description: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: Difficulty,
    val coverImageUrl: String?,
    val instructions: List<String>,
    val ingredients: List<RecipeIngredient>,
    val createdBy: UUID,
    val authorName: String,
    val viewCount: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PantryItem(
    val id: UUID,
    val userId: UUID,
    val ingredient: Ingredient,
    val quantity: BigDecimal,
    val unit: String,
    val isOutOfStock: Boolean,
    val updatedAt: Instant,
)

data class ShoppingItem(
    val id: UUID,
    val userId: UUID,
    val ingredientId: UUID?,
    val name: String,
    val quantity: BigDecimal?,
    val unit: String?,
    val source: ShoppingSource,
    val isPurchased: Boolean,
    val createdAt: Instant,
    val purchasedAt: Instant?,
)

/** Generic page of results used by every listing repository method. */
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
) {
    val totalPages: Int
        get() = if (size == 0) 0 else ((totalItems + size - 1) / size).toInt()
}
