package com.yeyint.recipeapp.shared.data.repository

import com.yeyint.recipeapp.shared.data.remote.api.RecipeApi
import com.yeyint.recipeapp.shared.data.remote.dto.toDetail
import com.yeyint.recipeapp.shared.data.remote.dto.toDomain
import com.yeyint.recipeapp.shared.data.remote.dto.toRequest
import com.yeyint.recipeapp.shared.data.remote.dto.toSummary
import com.yeyint.recipeapp.shared.domain.model.Page
import com.yeyint.recipeapp.shared.domain.model.RecipeDetail
import com.yeyint.recipeapp.shared.domain.model.RecipeDraft
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.repository.RecipeRepository
import com.yeyint.recipeapp.shared.domain.repository.RecipeSearchFilter
import com.yeyint.recipeapp.shared.util.AppResult
import com.yeyint.recipeapp.shared.util.map

class RecipeRepositoryImpl(private val api: RecipeApi) : RecipeRepository {

    override suspend fun search(
        filter: RecipeSearchFilter, page: Int, size: Int,
    ): AppResult<Page<RecipeSummary>> =
        api.search(filter, page, size).map { dto -> dto.toDomain { it.toSummary() } }

    override suspend fun detail(id: String): AppResult<RecipeDetail> =
        api.detail(id).map { it.toDetail() }

    override suspend fun create(draft: RecipeDraft): AppResult<RecipeDetail> =
        api.create(draft.toRequest()).map { it.toDetail() }

    override suspend fun update(id: String, draft: RecipeDraft): AppResult<RecipeDetail> =
        api.update(id, draft.toRequest()).map { it.toDetail() }

    override suspend fun delete(id: String): AppResult<Unit> =
        api.delete(id).map { }
}
