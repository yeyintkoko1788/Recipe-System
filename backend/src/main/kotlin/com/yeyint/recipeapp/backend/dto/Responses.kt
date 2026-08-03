package com.yeyint.recipeapp.backend.dto

import com.yeyint.recipeapp.backend.domain.Ingredient
import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.PantryItem
import com.yeyint.recipeapp.backend.domain.Recipe
import com.yeyint.recipeapp.backend.domain.ShoppingItem
import com.yeyint.recipeapp.backend.domain.User
import kotlinx.serialization.Serializable

/**
 * Uniform response envelope. Success: `{ success, data }`.
 * Failure: `{ success, error: { code, message, details } }`.
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
) {
    companion object {
        fun <T> ok(data: T) = ApiResponse(success = true, data = data)
        fun failure(code: String, message: String, details: Map<String, String>? = null) =
            ApiResponse<Unit>(success = false, error = ApiError(code, message, details))
    }
}

@Serializable
data class ApiError(val code: String, val message: String, val details: Map<String, String>? = null)

@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

fun <T, R> Page<T>.toResponse(transform: (T) -> R) =
    PageResponse(items.map(transform), page, size, totalItems, totalPages)

// ---------------------------------------------------------------------------
// Entity responses (timestamps serialized as ISO-8601 strings)
// ---------------------------------------------------------------------------

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val createdAt: String,
)

fun User.toResponse() = UserResponse(id.toString(), name, email, role.name, createdAt.toString())

@Serializable
data class AuthResponse(
    val user: UserResponse,
    val accessToken: String,
    val refreshToken: String,
    /** Access-token lifetime in seconds; clients refresh proactively. */
    val expiresInSeconds: Long,
)

@Serializable
data class IngredientResponse(
    val id: String,
    val name: String,
    val category: String,
    val defaultUnit: String,
    val imageUrl: String? = null,
    val status: String,
    val createdAt: String,
)

fun Ingredient.toResponse() =
    IngredientResponse(id.toString(), name, category, defaultUnit, imageUrl, status.name, createdAt.toString())

@Serializable
data class RecipeIngredientResponse(
    val ingredientId: String,
    val ingredientName: String,
    val quantity: Double,
    val unit: String,
    val note: String? = null,
)

@Serializable
data class RecipeResponse(
    val id: String,
    val title: String,
    val description: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: String,
    val coverImageUrl: String? = null,
    val instructions: List<String>,
    val ingredients: List<RecipeIngredientResponse>,
    val authorId: String,
    val authorName: String,
    val viewCount: Long,
    val createdAt: String,
    val updatedAt: String,
)

fun Recipe.toResponse() = RecipeResponse(
    id = id.toString(),
    title = title,
    description = description,
    cookingTimeMinutes = cookingTimeMinutes,
    servings = servings,
    difficulty = difficulty.name,
    coverImageUrl = coverImageUrl,
    instructions = instructions,
    ingredients = ingredients.map {
        RecipeIngredientResponse(
            it.ingredientId.toString(), it.ingredientName, it.quantity.toDouble(), it.unit, it.note,
        )
    },
    authorId = createdBy.toString(),
    authorName = authorName,
    viewCount = viewCount,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

@Serializable
data class PantryItemResponse(
    val id: String,
    val ingredient: IngredientResponse,
    val quantity: Double,
    val unit: String,
    val isOutOfStock: Boolean,
    val updatedAt: String,
)

fun PantryItem.toResponse() = PantryItemResponse(
    id.toString(), ingredient.toResponse(), quantity.toDouble(), unit, isOutOfStock, updatedAt.toString(),
)

@Serializable
data class ShoppingItemResponse(
    val id: String,
    val ingredientId: String? = null,
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val source: String,
    val isPurchased: Boolean,
    val createdAt: String,
)

fun ShoppingItem.toResponse() = ShoppingItemResponse(
    id.toString(), ingredientId?.toString(), name, quantity?.toDouble(), unit,
    source.name, isPurchased, createdAt.toString(),
)

@Serializable
data class StatsResponse(
    val totalUsers: Long,
    val totalRecipes: Long,
    val totalIngredients: Long,
    val pendingIngredients: Long,
    val recipesCreatedLast7Days: Long,
)

@Serializable
data class MessageResponse(val message: String)
