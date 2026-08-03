package com.yeyint.recipeapp.shared.data.local

import com.yeyint.recipeapp.shared.db.RecipeDatabase
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary

/** Feeds that are cached for offline reading. */
enum class CachedFeed { EXPLORE_POPULAR, EXPLORE_NEW, EXPLORE_RECOMMENDED }

/**
 * SQLDelight-backed cache of recipe summaries. Written on every successful
 * feed fetch and read as a fallback when the device is offline.
 */
class RecipeCacheDataSource(private val database: RecipeDatabase) {

    fun read(feed: CachedFeed): List<RecipeSummary> =
        database.recipeCacheQueries.selectFeed(feed.name).executeAsList().map { row ->
            RecipeSummary(
                id = row.id,
                title = row.title,
                description = row.description,
                cookingTimeMinutes = row.cookingTimeMinutes.toInt(),
                servings = row.servings.toInt(),
                difficulty = Difficulty.entries.firstOrNull { it.name == row.difficulty } ?: Difficulty.MEDIUM,
                coverImageUrl = row.coverImageUrl,
                authorName = row.authorName,
                viewCount = row.viewCount,
                createdAt = row.createdAt,
            )
        }

    fun replace(feed: CachedFeed, recipes: List<RecipeSummary>) {
        database.recipeCacheQueries.transaction {
            database.recipeCacheQueries.clearFeed(feed.name)
            recipes.forEachIndexed { index, recipe ->
                database.recipeCacheQueries.insertSummary(
                    id = recipe.id,
                    feed = feed.name,
                    title = recipe.title,
                    description = recipe.description,
                    cookingTimeMinutes = recipe.cookingTimeMinutes.toLong(),
                    servings = recipe.servings.toLong(),
                    difficulty = recipe.difficulty.name,
                    coverImageUrl = recipe.coverImageUrl,
                    authorName = recipe.authorName,
                    viewCount = recipe.viewCount,
                    createdAt = recipe.createdAt,
                    position = index.toLong(),
                )
            }
        }
    }

    fun clear() = database.recipeCacheQueries.clearAll()
}
