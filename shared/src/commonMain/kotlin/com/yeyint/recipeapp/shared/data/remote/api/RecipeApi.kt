package com.yeyint.recipeapp.shared.data.remote.api

import com.yeyint.recipeapp.shared.data.remote.dto.MessageDto
import com.yeyint.recipeapp.shared.data.remote.dto.PageDto
import com.yeyint.recipeapp.shared.data.remote.dto.RecipeDto
import com.yeyint.recipeapp.shared.data.remote.dto.RecipeRequestDto
import com.yeyint.recipeapp.shared.data.remote.safeApiCall
import com.yeyint.recipeapp.shared.domain.repository.RecipeSearchFilter
import com.yeyint.recipeapp.shared.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

/** Thin, stateless wrapper over the recipe endpoints. */
class RecipeApi(private val client: HttpClient) {

    suspend fun search(filter: RecipeSearchFilter, page: Int, size: Int): AppResult<PageDto<RecipeDto>> =
        safeApiCall {
            client.get("api/v1/recipes") {
                filter.query?.let { parameter("query", it) }
                filter.difficulty?.let { parameter("difficulty", it.name) }
                filter.maxCookingTimeMinutes?.let { parameter("maxTime", it) }
                if (filter.onlyMine) parameter("mine", "true")
                parameter("sort", filter.sortBy)
                parameter("order", if (filter.ascending) "asc" else "desc")
                parameter("page", page)
                parameter("size", size)
            }
        }

    suspend fun detail(id: String): AppResult<RecipeDto> =
        safeApiCall { client.get("api/v1/recipes/$id") }

    suspend fun create(request: RecipeRequestDto): AppResult<RecipeDto> =
        safeApiCall { client.post("api/v1/recipes") { setBody(request) } }

    suspend fun update(id: String, request: RecipeRequestDto): AppResult<RecipeDto> =
        safeApiCall { client.put("api/v1/recipes/$id") { setBody(request) } }

    suspend fun delete(id: String): AppResult<MessageDto> =
        safeApiCall { client.delete("api/v1/recipes/$id") }

    // Explore feeds

    suspend fun popular(page: Int, size: Int): AppResult<PageDto<RecipeDto>> =
        safeApiCall { client.get("api/v1/explore/popular") { parameter("page", page); parameter("size", size) } }

    suspend fun newest(page: Int, size: Int): AppResult<PageDto<RecipeDto>> =
        safeApiCall { client.get("api/v1/explore/new") { parameter("page", page); parameter("size", size) } }

    suspend fun recommended(limit: Int): AppResult<List<RecipeDto>> =
        safeApiCall { client.get("api/v1/explore/recommended") { parameter("limit", limit) } }
}
