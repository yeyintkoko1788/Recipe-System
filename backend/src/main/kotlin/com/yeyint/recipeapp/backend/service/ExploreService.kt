package com.yeyint.recipeapp.backend.service

import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.Recipe
import com.yeyint.recipeapp.backend.repository.RecipeFilter
import com.yeyint.recipeapp.backend.repository.RecipeRepository
import com.yeyint.recipeapp.backend.util.PageRequest
import com.yeyint.recipeapp.backend.util.SortDirection
import java.util.UUID

/**
 * Discovery feeds. "Popular" is ranked by view count and "new" by creation
 * date. "Recommended" uses a pantry-overlap heuristic (see
 * [RecipeRepository.recommendedFor]) and falls back to popular recipes for
 * users with an empty pantry — the ranking strategy can be upgraded without
 * touching the API surface.
 */
class ExploreService(private val recipes: RecipeRepository) {

    suspend fun popular(page: Int, size: Int): Page<Recipe> =
        recipes.search(
            RecipeFilter(),
            PageRequest(page, size, sortBy = "popularity", direction = SortDirection.DESC),
        )

    suspend fun newest(page: Int, size: Int): Page<Recipe> =
        recipes.search(
            RecipeFilter(),
            PageRequest(page, size, sortBy = "createdAt", direction = SortDirection.DESC),
        )

    suspend fun recommended(userId: UUID, limit: Int = 10): List<Recipe> {
        val matches = recipes.recommendedFor(userId, limit)
        if (matches.isNotEmpty()) return matches
        return popular(page = 0, size = limit).items
    }
}
