package com.yeyint.recipeapp.shared.data.repository

import com.yeyint.recipeapp.shared.data.remote.api.CatalogApi
import com.yeyint.recipeapp.shared.data.remote.dto.IngredientRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.PantryUpdateRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.PantryUpsertRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.PurchaseRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.ShoppingAddRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.toDomain
import com.yeyint.recipeapp.shared.domain.model.Ingredient
import com.yeyint.recipeapp.shared.domain.model.Page
import com.yeyint.recipeapp.shared.domain.model.PantryItem
import com.yeyint.recipeapp.shared.domain.model.ShoppingItem
import com.yeyint.recipeapp.shared.domain.repository.IngredientRepository
import com.yeyint.recipeapp.shared.domain.repository.PantryRepository
import com.yeyint.recipeapp.shared.domain.repository.ShoppingRepository
import com.yeyint.recipeapp.shared.util.AppResult
import com.yeyint.recipeapp.shared.util.map

class IngredientRepositoryImpl(private val api: CatalogApi) : IngredientRepository {

    override suspend fun list(search: String?, page: Int, size: Int): AppResult<Page<Ingredient>> =
        api.ingredients(search, page, size).map { dto -> dto.toDomain { it.toDomain() } }

    override suspend fun submit(name: String, category: String, defaultUnit: String): AppResult<Ingredient> =
        api.submitIngredient(IngredientRequestDto(name, category, defaultUnit)).map { it.toDomain() }
}

class PantryRepositoryImpl(private val api: CatalogApi) : PantryRepository {

    override suspend fun items(): AppResult<List<PantryItem>> =
        api.pantry().map { list -> list.map { it.toDomain() } }

    override suspend fun add(ingredientId: String, quantity: Double, unit: String?): AppResult<PantryItem> =
        api.addPantryItem(PantryUpsertRequestDto(ingredientId, quantity, unit)).map { it.toDomain() }

    override suspend fun update(
        itemId: String, quantity: Double?, unit: String?, isOutOfStock: Boolean?,
    ): AppResult<PantryItem> =
        api.updatePantryItem(itemId, PantryUpdateRequestDto(quantity, unit, isOutOfStock)).map { it.toDomain() }

    override suspend fun markOutOfStock(itemId: String): AppResult<PantryItem> =
        api.markOutOfStock(itemId).map { it.toDomain() }

    override suspend fun restock(itemId: String, quantity: Double): AppResult<PantryItem> =
        api.restock(itemId, quantity).map { it.toDomain() }

    override suspend fun remove(itemId: String): AppResult<Unit> =
        api.removePantryItem(itemId).map { }
}

class ShoppingRepositoryImpl(private val api: CatalogApi) : ShoppingRepository {

    override suspend fun items(includePurchased: Boolean): AppResult<List<ShoppingItem>> =
        api.shoppingList(includePurchased).map { list -> list.map { it.toDomain() } }

    override suspend fun addManual(name: String, quantity: Double?, unit: String?): AppResult<ShoppingItem> =
        api.addShoppingItem(ShoppingAddRequestDto(name = name, quantity = quantity, unit = unit)).map { it.toDomain() }

    override suspend fun generateFromPantry(): AppResult<List<ShoppingItem>> =
        api.generateShoppingList().map { list -> list.map { it.toDomain() } }

    override suspend fun purchase(itemId: String, restockPantry: Boolean, quantity: Double?): AppResult<ShoppingItem> =
        api.purchase(itemId, PurchaseRequestDto(restockPantry, quantity)).map { it.toDomain() }

    override suspend fun remove(itemId: String): AppResult<Unit> =
        api.removeShoppingItem(itemId).map { }

    override suspend fun clearPurchased(): AppResult<Unit> =
        api.clearPurchased().map { }
}
