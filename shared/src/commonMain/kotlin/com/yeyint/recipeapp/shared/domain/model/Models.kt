package com.yeyint.recipeapp.shared.domain.model

enum class UserRole { ADMIN, USER }
enum class Difficulty { EASY, MEDIUM, HARD }
enum class IngredientStatus { APPROVED, PENDING, REJECTED }
enum class ShoppingSource { MANUAL, PANTRY }

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
)

data class Ingredient(
    val id: String,
    val name: String,
    val category: String,
    val defaultUnit: String,
    val imageUrl: String?,
    val status: IngredientStatus,
)

/** Lightweight recipe used in lists and feeds. */
data class RecipeSummary(
    val id: String,
    val title: String,
    val description: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: Difficulty,
    val coverImageUrl: String?,
    val authorName: String,
    val viewCount: Long,
    val createdAt: String,
)

data class RecipeIngredientLine(
    val ingredientId: String,
    val ingredientName: String,
    val quantity: Double,
    val unit: String,
    val note: String?,
)

/** Full recipe as shown on the detail screen. */
data class RecipeDetail(
    val id: String,
    val title: String,
    val description: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: Difficulty,
    val coverImageUrl: String?,
    val instructions: List<String>,
    val ingredients: List<RecipeIngredientLine>,
    val authorId: String,
    val authorName: String,
    val viewCount: Long,
) {
    fun toSummary() = RecipeSummary(
        id, title, description, cookingTimeMinutes, servings, difficulty,
        coverImageUrl, authorName, viewCount, createdAt = "",
    )
}

/** Editable recipe payload used by the create/edit screens. */
data class RecipeDraft(
    val title: String,
    val description: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: Difficulty,
    val coverImageUrl: String? = null,
    val instructions: List<String>,
    val ingredients: List<DraftIngredient>,
) {
    data class DraftIngredient(
        val ingredientId: String,
        val ingredientName: String,
        val quantity: Double,
        val unit: String,
        val note: String? = null,
    )
}

data class PantryItem(
    val id: String,
    val ingredient: Ingredient,
    val quantity: Double,
    val unit: String,
    val isOutOfStock: Boolean,
)

data class ShoppingItem(
    val id: String,
    val ingredientId: String?,
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val source: ShoppingSource,
    val isPurchased: Boolean,
)

data class Page<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = page + 1 < totalPages

    fun <R> map(transform: (T) -> R) = Page(items.map(transform), page, size, totalItems, totalPages)

    companion object {
        fun <T> empty() = Page<T>(emptyList(), 0, 0, 0, 0)
    }
}
