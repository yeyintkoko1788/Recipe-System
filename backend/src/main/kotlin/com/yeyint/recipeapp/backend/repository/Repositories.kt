package com.yeyint.recipeapp.backend.repository

import com.yeyint.recipeapp.backend.domain.Difficulty
import com.yeyint.recipeapp.backend.domain.Ingredient
import com.yeyint.recipeapp.backend.domain.IngredientStatus
import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.PantryItem
import com.yeyint.recipeapp.backend.domain.Recipe
import com.yeyint.recipeapp.backend.domain.ShoppingItem
import com.yeyint.recipeapp.backend.domain.ShoppingSource
import com.yeyint.recipeapp.backend.domain.User
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.util.PageRequest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Repository interfaces — the persistence boundary of the backend. Services
 * depend on these abstractions only; the Exposed implementations live in
 * [com.yeyint.recipeapp.backend.repository.exposed].
 */

interface UserRepository {
    suspend fun findByEmail(email: String): User?
    suspend fun findById(id: UUID): User?
    suspend fun create(name: String, email: String, passwordHash: String, role: UserRole): User
    suspend fun list(page: PageRequest, search: String?): Page<User>
    suspend fun updateRole(id: UUID, role: UserRole): Boolean
    suspend fun delete(id: UUID): Boolean
}

interface RefreshTokenRepository {
    suspend fun store(userId: UUID, tokenHash: String, expiresAt: Instant)
    /** Returns the owning user id when the token exists, is unrevoked and unexpired. */
    suspend fun findUserIdByValidToken(tokenHash: String, now: Instant): UUID?
    suspend fun revoke(tokenHash: String): Boolean
    suspend fun revokeAllForUser(userId: UUID)
}

interface IngredientRepository {
    suspend fun findById(id: UUID): Ingredient?
    /** Case-insensitive lookup — enforces the "Salt == salt == SALT" rule. */
    suspend fun findByName(name: String): Ingredient?
    /** Of the given ids, returns the subset that exists. */
    suspend fun existingIds(ids: Collection<UUID>): Set<UUID>
    suspend fun create(
        name: String, category: String, defaultUnit: String,
        imageUrl: String?, status: IngredientStatus, createdBy: UUID?,
    ): Ingredient
    suspend fun update(id: UUID, name: String, category: String, defaultUnit: String, imageUrl: String?): Ingredient?
    suspend fun updateStatus(id: UUID, status: IngredientStatus): Boolean
    suspend fun delete(id: UUID): Boolean
    suspend fun list(
        page: PageRequest, search: String?, category: String?, status: IngredientStatus?,
    ): Page<Ingredient>
}

data class RecipeFilter(
    val query: String? = null,
    val difficulty: Difficulty? = null,
    val maxCookingTimeMinutes: Int? = null,
    val createdBy: UUID? = null,
)

data class RecipeIngredientData(
    val ingredientId: UUID,
    val quantity: BigDecimal,
    val unit: String,
    val note: String?,
)

data class RecipeData(
    val title: String,
    val description: String,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val difficulty: Difficulty,
    val coverImageUrl: String?,
    val instructions: List<String>,
    val ingredients: List<RecipeIngredientData>,
)

interface RecipeRepository {
    /** Full recipe including ingredient lines; null when missing or soft-deleted. */
    suspend fun findById(id: UUID): Recipe?
    suspend fun create(ownerId: UUID, data: RecipeData): Recipe
    suspend fun update(id: UUID, data: RecipeData): Recipe?
    suspend fun softDelete(id: UUID): Boolean
    /** Paged summaries (ingredients not loaded) matching [filter]. */
    suspend fun search(filter: RecipeFilter, page: PageRequest): Page<Recipe>
    suspend fun incrementViewCount(id: UUID)
    /**
     * Basic content-based recommendation: recipes (not authored by the user)
     * ranked by how many of their ingredients the user already has in the
     * pantry. Swappable for a smarter engine later without API changes.
     */
    suspend fun recommendedFor(userId: UUID, limit: Int): List<Recipe>
}

interface PantryRepository {
    suspend fun listForUser(userId: UUID): List<PantryItem>
    suspend fun find(userId: UUID, id: UUID): PantryItem?
    suspend fun findByIngredient(userId: UUID, ingredientId: UUID): PantryItem?
    suspend fun upsert(userId: UUID, ingredientId: UUID, quantity: BigDecimal, unit: String): PantryItem
    suspend fun update(
        userId: UUID, id: UUID,
        quantity: BigDecimal?, unit: String?, isOutOfStock: Boolean?,
    ): PantryItem?
    suspend fun delete(userId: UUID, id: UUID): Boolean
    suspend fun outOfStockItems(userId: UUID): List<PantryItem>
}

interface ShoppingRepository {
    suspend fun listForUser(userId: UUID, includePurchased: Boolean): List<ShoppingItem>
    suspend fun find(userId: UUID, id: UUID): ShoppingItem?
    suspend fun add(
        userId: UUID, ingredientId: UUID?, name: String,
        quantity: BigDecimal?, unit: String?, source: ShoppingSource,
    ): ShoppingItem
    suspend fun existsUnpurchasedForIngredient(userId: UUID, ingredientId: UUID): Boolean
    suspend fun markPurchased(userId: UUID, id: UUID): ShoppingItem?
    suspend fun delete(userId: UUID, id: UUID): Boolean
    suspend fun clearPurchased(userId: UUID): Int
}

data class StatsSnapshot(
    val totalUsers: Long,
    val totalRecipes: Long,
    val totalIngredients: Long,
    val pendingIngredients: Long,
    val recipesCreatedLast7Days: Long,
)

interface StatsRepository {
    suspend fun snapshot(): StatsSnapshot
}
