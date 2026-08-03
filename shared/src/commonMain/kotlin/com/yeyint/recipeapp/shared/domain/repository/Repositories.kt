package com.yeyint.recipeapp.shared.domain.repository

import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.Ingredient
import com.yeyint.recipeapp.shared.domain.model.Page
import com.yeyint.recipeapp.shared.domain.model.PantryItem
import com.yeyint.recipeapp.shared.domain.model.RecipeDetail
import com.yeyint.recipeapp.shared.domain.model.RecipeDraft
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.model.ShoppingItem
import com.yeyint.recipeapp.shared.domain.model.User
import com.yeyint.recipeapp.shared.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Authentication state observed by the UI to decide the start destination. */
sealed interface AuthState {
    data object Unknown : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val user: User) : AuthState
}

interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun restoreSession()
    suspend fun login(email: String, password: String): AppResult<User>
    suspend fun register(name: String, email: String, password: String): AppResult<User>
    suspend fun logout(): AppResult<Unit>
    /** Called by the network layer when the refresh token is rejected. */
    fun onSessionExpired()
}

data class RecipeSearchFilter(
    val query: String? = null,
    val difficulty: Difficulty? = null,
    val maxCookingTimeMinutes: Int? = null,
    val onlyMine: Boolean = false,
    /** One of: createdAt, title, cookingTime, popularity. */
    val sortBy: String = "createdAt",
    val ascending: Boolean = false,
)

interface RecipeRepository {
    suspend fun search(filter: RecipeSearchFilter, page: Int, size: Int = 20): AppResult<Page<RecipeSummary>>
    suspend fun detail(id: String): AppResult<RecipeDetail>
    suspend fun create(draft: RecipeDraft): AppResult<RecipeDetail>
    suspend fun update(id: String, draft: RecipeDraft): AppResult<RecipeDetail>
    suspend fun delete(id: String): AppResult<Unit>
}

interface ExploreRepository {
    suspend fun popular(page: Int, size: Int = 10): AppResult<Page<RecipeSummary>>
    suspend fun newest(page: Int, size: Int = 10): AppResult<Page<RecipeSummary>>
    suspend fun recommended(limit: Int = 10): AppResult<List<RecipeSummary>>
}

interface IngredientRepository {
    suspend fun list(search: String?, page: Int, size: Int = 30): AppResult<Page<Ingredient>>
    /** User submission — lands in the catalog as PENDING until approved. */
    suspend fun submit(name: String, category: String, defaultUnit: String): AppResult<Ingredient>
}

interface PantryRepository {
    suspend fun items(): AppResult<List<PantryItem>>
    suspend fun add(ingredientId: String, quantity: Double, unit: String?): AppResult<PantryItem>
    suspend fun update(itemId: String, quantity: Double?, unit: String?, isOutOfStock: Boolean?): AppResult<PantryItem>
    suspend fun markOutOfStock(itemId: String): AppResult<PantryItem>
    suspend fun restock(itemId: String, quantity: Double): AppResult<PantryItem>
    suspend fun remove(itemId: String): AppResult<Unit>
}

interface ShoppingRepository {
    suspend fun items(includePurchased: Boolean): AppResult<List<ShoppingItem>>
    suspend fun addManual(name: String, quantity: Double?, unit: String?): AppResult<ShoppingItem>
    suspend fun generateFromPantry(): AppResult<List<ShoppingItem>>
    suspend fun purchase(itemId: String, restockPantry: Boolean, quantity: Double?): AppResult<ShoppingItem>
    suspend fun remove(itemId: String): AppResult<Unit>
    suspend fun clearPurchased(): AppResult<Unit>
}

/** Local app preferences (theme, …) persisted on device. */
interface SettingsRepository {
    /** null = follow system. */
    val darkMode: Flow<Boolean?>
    suspend fun setDarkMode(enabled: Boolean?)
}
