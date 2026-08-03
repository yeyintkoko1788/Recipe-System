package com.yeyint.recipeapp.backend.repository.exposed

import com.yeyint.recipeapp.backend.config.dbQuery
import com.yeyint.recipeapp.backend.db.IngredientsTable
import com.yeyint.recipeapp.backend.db.PantryItemsTable
import com.yeyint.recipeapp.backend.db.RecipeIngredientsTable
import com.yeyint.recipeapp.backend.db.RecipesTable
import com.yeyint.recipeapp.backend.db.UsersTable
import com.yeyint.recipeapp.backend.domain.Difficulty
import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.Recipe
import com.yeyint.recipeapp.backend.domain.RecipeIngredient
import com.yeyint.recipeapp.backend.repository.RecipeData
import com.yeyint.recipeapp.backend.repository.RecipeFilter
import com.yeyint.recipeapp.backend.repository.RecipeRepository
import com.yeyint.recipeapp.backend.util.PageRequest
import com.yeyint.recipeapp.backend.util.SortDirection
import org.jetbrains.exposed.sql.Count
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedRecipeRepository : RecipeRepository {

    override suspend fun findById(id: UUID): Recipe? = dbQuery {
        val row = recipesWithAuthor()
            .where { (RecipesTable.id eq id) and notDeleted() }
            .singleOrNull() ?: return@dbQuery null
        row.toRecipe(ingredientsOf(id))
    }

    override suspend fun create(ownerId: UUID, data: RecipeData): Recipe = dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val recipeId = RecipesTable.insert {
            it[title] = data.title
            it[description] = data.description
            it[cookingTimeMinutes] = data.cookingTimeMinutes
            it[servings] = data.servings
            it[difficulty] = data.difficulty.name
            it[coverImageUrl] = data.coverImageUrl
            it[instructions] = data.instructions
            it[createdBy] = ownerId
            it[createdAt] = now
            it[updatedAt] = now
        } get RecipesTable.id
        insertIngredientLines(recipeId.value, data)
        val row = recipesWithAuthor().where { RecipesTable.id eq recipeId }.single()
        row.toRecipe(ingredientsOf(recipeId.value))
    }

    override suspend fun update(id: UUID, data: RecipeData): Recipe? = dbQuery {
        val updated = RecipesTable.update({ (RecipesTable.id eq id) and notDeleted() }) {
            it[title] = data.title
            it[description] = data.description
            it[cookingTimeMinutes] = data.cookingTimeMinutes
            it[servings] = data.servings
            it[difficulty] = data.difficulty.name
            it[coverImageUrl] = data.coverImageUrl
            it[instructions] = data.instructions
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        if (updated == 0) return@dbQuery null
        // Replace ingredient lines wholesale — simplest correct strategy.
        RecipeIngredientsTable.deleteWhere { recipeId eq id }
        insertIngredientLines(id, data)
        val row = recipesWithAuthor().where { RecipesTable.id eq id }.single()
        row.toRecipe(ingredientsOf(id))
    }

    override suspend fun softDelete(id: UUID): Boolean = dbQuery {
        RecipesTable.update({ (RecipesTable.id eq id) and notDeleted() }) {
            it[isDeleted] = true
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        } > 0
    }

    override suspend fun search(filter: RecipeFilter, page: PageRequest): Page<Recipe> = dbQuery {
        val query = recipesWithAuthor().where { notDeleted() }
        filter.query?.takeIf { it.isNotBlank() }?.let { q ->
            val term = "%${q.lowercase()}%"
            query.andWhere {
                (RecipesTable.title.lowerCase() like term) or
                    (RecipesTable.description.lowerCase() like term)
            }
        }
        filter.difficulty?.let { query.andWhere { RecipesTable.difficulty eq it.name } }
        filter.maxCookingTimeMinutes?.let { query.andWhere { RecipesTable.cookingTimeMinutes lessEq it } }
        filter.createdBy?.let { query.andWhere { RecipesTable.createdBy eq it } }

        val total = query.count()
        val sortColumn = when (page.sortBy) {
            "title" -> RecipesTable.title
            "cookingTime" -> RecipesTable.cookingTimeMinutes
            "popularity" -> RecipesTable.viewCount
            else -> RecipesTable.createdAt
        }
        val items = query
            .orderBy(sortColumn, if (page.direction == SortDirection.ASC) SortOrder.ASC else SortOrder.DESC)
            .limit(page.size).offset(page.offset)
            .map { it.toRecipe(emptyList()) }
        Page(items, page.page, page.size, total)
    }

    override suspend fun incrementViewCount(id: UUID): Unit = dbQuery {
        RecipesTable.update({ RecipesTable.id eq id }) {
            it[viewCount] = viewCount + 1
        }
    }

    override suspend fun recommendedFor(userId: UUID, limit: Int): List<Recipe> = dbQuery {
        // Rank recipes by number of matching in-stock pantry ingredients.
        val matches = Count(RecipeIngredientsTable.ingredientId)
        val rankedIds = RecipeIngredientsTable
            .join(
                PantryItemsTable, JoinType.INNER,
                additionalConstraint = {
                    (RecipeIngredientsTable.ingredientId eq PantryItemsTable.ingredientId) and
                        (PantryItemsTable.userId eq userId) and
                        (PantryItemsTable.isOutOfStock eq false)
                },
            )
            .join(RecipesTable, JoinType.INNER, RecipeIngredientsTable.recipeId, RecipesTable.id)
            .select(RecipeIngredientsTable.recipeId, matches)
            .where { (RecipesTable.isDeleted eq false) and (RecipesTable.createdBy neq userId) }
            .groupBy(RecipeIngredientsTable.recipeId)
            .orderBy(matches, SortOrder.DESC)
            .limit(limit)
            .map { it[RecipeIngredientsTable.recipeId].value }

        if (rankedIds.isEmpty()) return@dbQuery emptyList()
        val byId = recipesWithAuthor()
            .where { RecipesTable.id inList rankedIds }
            .associateBy({ it[RecipesTable.id].value }, { it.toRecipe(emptyList()) })
        rankedIds.mapNotNull { byId[it] } // preserve ranking order
    }

    // -- helpers ------------------------------------------------------------

    private fun recipesWithAuthor() =
        RecipesTable.join(UsersTable, JoinType.INNER, RecipesTable.createdBy, UsersTable.id)
            .select(RecipesTable.columns + UsersTable.name)

    private fun notDeleted() = RecipesTable.isDeleted eq false

    private fun insertIngredientLines(recipeId: UUID, data: RecipeData) {
        data.ingredients.forEach { line ->
            RecipeIngredientsTable.insert {
                it[RecipeIngredientsTable.recipeId] = recipeId
                it[ingredientId] = line.ingredientId
                it[quantity] = line.quantity
                it[unit] = line.unit
                it[note] = line.note
            }
        }
    }

    private fun ingredientsOf(recipeId: UUID): List<RecipeIngredient> =
        RecipeIngredientsTable
            .join(IngredientsTable, JoinType.INNER, RecipeIngredientsTable.ingredientId, IngredientsTable.id)
            .select(
                RecipeIngredientsTable.ingredientId, IngredientsTable.name,
                RecipeIngredientsTable.quantity, RecipeIngredientsTable.unit, RecipeIngredientsTable.note,
            )
            .where { RecipeIngredientsTable.recipeId eq recipeId }
            .map {
                RecipeIngredient(
                    ingredientId = it[RecipeIngredientsTable.ingredientId].value,
                    ingredientName = it[IngredientsTable.name],
                    quantity = it[RecipeIngredientsTable.quantity],
                    unit = it[RecipeIngredientsTable.unit],
                    note = it[RecipeIngredientsTable.note],
                )
            }

    private fun ResultRow.toRecipe(ingredients: List<RecipeIngredient>) = Recipe(
        id = this[RecipesTable.id].value,
        title = this[RecipesTable.title],
        description = this[RecipesTable.description],
        cookingTimeMinutes = this[RecipesTable.cookingTimeMinutes],
        servings = this[RecipesTable.servings],
        difficulty = Difficulty.valueOf(this[RecipesTable.difficulty]),
        coverImageUrl = this[RecipesTable.coverImageUrl],
        instructions = this[RecipesTable.instructions],
        ingredients = ingredients,
        createdBy = this[RecipesTable.createdBy].value,
        authorName = this[UsersTable.name],
        viewCount = this[RecipesTable.viewCount],
        createdAt = this[RecipesTable.createdAt].toInstant(),
        updatedAt = this[RecipesTable.updatedAt].toInstant(),
    )
}
