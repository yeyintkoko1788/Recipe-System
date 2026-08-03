package com.yeyint.recipeapp.shared.data.repository

import com.yeyint.recipeapp.shared.data.local.CachedFeed
import com.yeyint.recipeapp.shared.data.local.RecipeCacheDataSource
import com.yeyint.recipeapp.shared.data.remote.api.RecipeApi
import com.yeyint.recipeapp.shared.data.remote.dto.toDomain
import com.yeyint.recipeapp.shared.data.remote.dto.toSummary
import com.yeyint.recipeapp.shared.domain.model.Page
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.repository.ExploreRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.AppResult
import com.yeyint.recipeapp.shared.util.map
import com.yeyint.recipeapp.shared.util.onSuccess

/**
 * Explore feeds with an offline fallback: the first page of every feed is
 * cached locally; when the network is unreachable the cached copy is served
 * instead of an error.
 */
class ExploreRepositoryImpl(
    private val api: RecipeApi,
    private val cache: RecipeCacheDataSource,
) : ExploreRepository {

    override suspend fun popular(page: Int, size: Int): AppResult<Page<RecipeSummary>> =
        cachedFeed(CachedFeed.EXPLORE_POPULAR, page) { api.popular(page, size).map { dto -> dto.toDomain { it.toSummary() } } }

    override suspend fun newest(page: Int, size: Int): AppResult<Page<RecipeSummary>> =
        cachedFeed(CachedFeed.EXPLORE_NEW, page) { api.newest(page, size).map { dto -> dto.toDomain { it.toSummary() } } }

    override suspend fun recommended(limit: Int): AppResult<List<RecipeSummary>> {
        val result = api.recommended(limit).map { list -> list.map { it.toSummary() } }
            .onSuccess { cache.replace(CachedFeed.EXPLORE_RECOMMENDED, it) }
        return result.orCached { cache.read(CachedFeed.EXPLORE_RECOMMENDED) }
    }

    private suspend fun cachedFeed(
        feed: CachedFeed,
        page: Int,
        fetch: suspend () -> AppResult<Page<RecipeSummary>>,
    ): AppResult<Page<RecipeSummary>> {
        val result = fetch().onSuccess { if (page == 0) cache.replace(feed, it.items) }
        if (page > 0) return result
        return when {
            result is AppResult.Failure && result.error is AppError.Network -> {
                val cached = cache.read(feed)
                if (cached.isEmpty()) result
                else AppResult.Success(Page(cached, 0, cached.size, cached.size.toLong(), 1))
            }
            else -> result
        }
    }

    private fun <T> AppResult<List<T>>.orCached(read: () -> List<T>): AppResult<List<T>> =
        if (this is AppResult.Failure && error is AppError.Network) {
            val cached = read()
            if (cached.isEmpty()) this else AppResult.Success(cached)
        } else {
            this
        }
}
