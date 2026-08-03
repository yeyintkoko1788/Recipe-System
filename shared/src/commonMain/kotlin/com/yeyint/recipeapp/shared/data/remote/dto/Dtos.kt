package com.yeyint.recipeapp.shared.data.remote.dto

import kotlinx.serialization.Serializable

/** Mirror of the backend response envelope. */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorDto? = null,
)

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)

@Serializable
data class PageDto<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

// Auth ----------------------------------------------------------------------

@Serializable
data class UserDto(val id: String, val name: String, val email: String, val role: String, val createdAt: String)

@Serializable
data class AuthResponseDto(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)

@Serializable
data class RegisterRequestDto(val name: String, val email: String, val password: String)

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class RefreshTokenRequestDto(val refreshToken: String)

// Catalog / recipes ---------------------------------------------------------

@Serializable
data class IngredientDto(
    val id: String,
    val name: String,
    val category: String,
    val defaultUnit: String,
    val imageUrl: String? = null,
    val status: String,
    val createdAt: String,
)

@Serializable
data class IngredientRequestDto(
    val name: String,
    val category: String,
    val defaultUnit: String,
    val imageUrl: String? = null,
)

@Serializable
data class RecipeIngredientDto(
    val ingredientId: String,
    val ingredientName: String,
    val quantity: Double,
    val unit: String,
    val note: String? = null,
)

@Serializable
data class RecipeDto(
    val id: String,
    val title: String,
    val description: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: String,
    val coverImageUrl: String? = null,
    val instructions: List<String> = emptyList(),
    val ingredients: List<RecipeIngredientDto> = emptyList(),
    val authorId: String,
    val authorName: String,
    val viewCount: Long,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class RecipeIngredientRequestDto(
    val ingredientId: String,
    val quantity: Double,
    val unit: String,
    val note: String? = null,
)

@Serializable
data class RecipeRequestDto(
    val title: String,
    val description: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: String,
    val coverImageUrl: String? = null,
    val instructions: List<String>,
    val ingredients: List<RecipeIngredientRequestDto>,
)

// Pantry / shopping ---------------------------------------------------------

@Serializable
data class PantryItemDto(
    val id: String,
    val ingredient: IngredientDto,
    val quantity: Double,
    val unit: String,
    val isOutOfStock: Boolean,
    val updatedAt: String,
)

@Serializable
data class PantryUpsertRequestDto(val ingredientId: String, val quantity: Double, val unit: String? = null)

@Serializable
data class PantryUpdateRequestDto(
    val quantity: Double? = null,
    val unit: String? = null,
    val isOutOfStock: Boolean? = null,
)

@Serializable
data class RestockRequestDto(val quantity: Double)

@Serializable
data class ShoppingItemDto(
    val id: String,
    val ingredientId: String? = null,
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val source: String,
    val isPurchased: Boolean,
    val createdAt: String,
)

@Serializable
data class ShoppingAddRequestDto(
    val ingredientId: String? = null,
    val name: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
)

@Serializable
data class PurchaseRequestDto(val restockPantry: Boolean = true, val quantity: Double? = null)

@Serializable
data class MessageDto(val message: String)
