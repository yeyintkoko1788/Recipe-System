package com.yeyint.recipeapp.shared.data.remote.api

import com.yeyint.recipeapp.shared.data.remote.dto.IngredientDto
import com.yeyint.recipeapp.shared.data.remote.dto.IngredientRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.MessageDto
import com.yeyint.recipeapp.shared.data.remote.dto.PageDto
import com.yeyint.recipeapp.shared.data.remote.dto.PantryItemDto
import com.yeyint.recipeapp.shared.data.remote.dto.PantryUpdateRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.PantryUpsertRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.PurchaseRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.RestockRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.ShoppingAddRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.ShoppingItemDto
import com.yeyint.recipeapp.shared.data.remote.safeApiCall
import com.yeyint.recipeapp.shared.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Ingredient catalog, pantry and shopping-list endpoints. */
class CatalogApi(private val client: HttpClient) {

    // Ingredients -----------------------------------------------------------

    suspend fun ingredients(search: String?, page: Int, size: Int): AppResult<PageDto<IngredientDto>> =
        safeApiCall {
            client.get("api/v1/ingredients") {
                search?.let { parameter("search", it) }
                parameter("page", page)
                parameter("size", size)
            }
        }

    suspend fun submitIngredient(request: IngredientRequestDto): AppResult<IngredientDto> =
        safeApiCall { client.post("api/v1/ingredients") { setBody(request) } }

    // Pantry ----------------------------------------------------------------

    suspend fun pantry(): AppResult<List<PantryItemDto>> =
        safeApiCall { client.get("api/v1/pantry") }

    suspend fun addPantryItem(request: PantryUpsertRequestDto): AppResult<PantryItemDto> =
        safeApiCall { client.post("api/v1/pantry") { setBody(request) } }

    suspend fun updatePantryItem(id: String, request: PantryUpdateRequestDto): AppResult<PantryItemDto> =
        safeApiCall { client.patch("api/v1/pantry/$id") { setBody(request) } }

    suspend fun markOutOfStock(id: String): AppResult<PantryItemDto> =
        safeApiCall { client.post("api/v1/pantry/$id/out-of-stock") }

    suspend fun restock(id: String, quantity: Double): AppResult<PantryItemDto> =
        safeApiCall { client.post("api/v1/pantry/$id/restock") { setBody(RestockRequestDto(quantity)) } }

    suspend fun removePantryItem(id: String): AppResult<MessageDto> =
        safeApiCall { client.delete("api/v1/pantry/$id") }

    // Shopping list ---------------------------------------------------------

    suspend fun shoppingList(includePurchased: Boolean): AppResult<List<ShoppingItemDto>> =
        safeApiCall {
            client.get("api/v1/shopping-list") { parameter("includePurchased", includePurchased) }
        }

    suspend fun addShoppingItem(request: ShoppingAddRequestDto): AppResult<ShoppingItemDto> =
        safeApiCall { client.post("api/v1/shopping-list") { setBody(request) } }

    suspend fun generateShoppingList(): AppResult<List<ShoppingItemDto>> =
        safeApiCall { client.post("api/v1/shopping-list/generate") }

    suspend fun purchase(id: String, request: PurchaseRequestDto): AppResult<ShoppingItemDto> =
        safeApiCall { client.post("api/v1/shopping-list/$id/purchase") { setBody(request) } }

    suspend fun removeShoppingItem(id: String): AppResult<MessageDto> =
        safeApiCall { client.delete("api/v1/shopping-list/$id") }

    suspend fun clearPurchased(): AppResult<MessageDto> =
        safeApiCall { client.delete("api/v1/shopping-list/purchased") }
}
