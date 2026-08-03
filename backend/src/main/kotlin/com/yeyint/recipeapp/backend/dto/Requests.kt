package com.yeyint.recipeapp.backend.dto

import kotlinx.serialization.Serializable

// Auth ----------------------------------------------------------------------

@Serializable
data class RegisterRequest(val name: String, val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

// Ingredients ---------------------------------------------------------------

@Serializable
data class IngredientRequest(
    val name: String,
    val category: String,
    val defaultUnit: String,
    val imageUrl: String? = null,
)

// Recipes -------------------------------------------------------------------

@Serializable
data class RecipeIngredientRequest(
    val ingredientId: String,
    val quantity: Double,
    val unit: String,
    val note: String? = null,
)

@Serializable
data class RecipeRequest(
    val title: String,
    val description: String = "",
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: String,
    val coverImageUrl: String? = null,
    val instructions: List<String>,
    val ingredients: List<RecipeIngredientRequest>,
)

// Pantry --------------------------------------------------------------------

@Serializable
data class PantryUpsertRequest(
    val ingredientId: String,
    val quantity: Double,
    val unit: String? = null,
)

@Serializable
data class PantryUpdateRequest(
    val quantity: Double? = null,
    val unit: String? = null,
    val isOutOfStock: Boolean? = null,
)

// Shopping ------------------------------------------------------------------

@Serializable
data class ShoppingAddRequest(
    val ingredientId: String? = null,
    val name: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
)

@Serializable
data class PurchaseRequest(
    /** When true (default) purchasing restores the linked pantry item's stock. */
    val restockPantry: Boolean = true,
    val quantity: Double? = null,
)

// Admin ---------------------------------------------------------------------

@Serializable
data class UpdateUserRoleRequest(val role: String)

@Serializable
data class ModerateIngredientRequest(val status: String)

@Serializable
data class RestockRequest(val quantity: Double)
