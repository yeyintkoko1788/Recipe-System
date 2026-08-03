package com.yeyint.recipeapp.backend.service

import com.yeyint.recipeapp.backend.domain.Difficulty
import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.Recipe
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.dto.RecipeRequest
import com.yeyint.recipeapp.backend.repository.IngredientRepository
import com.yeyint.recipeapp.backend.repository.RecipeData
import com.yeyint.recipeapp.backend.repository.RecipeFilter
import com.yeyint.recipeapp.backend.repository.RecipeIngredientData
import com.yeyint.recipeapp.backend.repository.RecipeRepository
import com.yeyint.recipeapp.backend.util.AppException
import com.yeyint.recipeapp.backend.util.PageRequest
import com.yeyint.recipeapp.backend.util.validate
import java.math.BigDecimal
import java.util.UUID

/** Recipe CRUD + search, with ownership checks (author or admin). */
class RecipeService(
    private val recipes: RecipeRepository,
    private val ingredients: IngredientRepository,
) {

    suspend fun search(filter: RecipeFilter, page: PageRequest): Page<Recipe> =
        recipes.search(filter, page)

    /** Fetches a recipe and counts the view (best-effort popularity signal). */
    suspend fun get(id: UUID, countView: Boolean = true): Recipe {
        val recipe = recipes.findById(id) ?: throw AppException.NotFound("Recipe not found")
        if (countView) recipes.incrementViewCount(id)
        return recipe
    }

    suspend fun create(ownerId: UUID, request: RecipeRequest): Recipe =
        recipes.create(ownerId, toData(request))

    suspend fun update(id: UUID, callerId: UUID, callerRole: UserRole, request: RecipeRequest): Recipe {
        assertCanModify(id, callerId, callerRole)
        return recipes.update(id, toData(request)) ?: throw AppException.NotFound("Recipe not found")
    }

    suspend fun delete(id: UUID, callerId: UUID, callerRole: UserRole) {
        assertCanModify(id, callerId, callerRole)
        if (!recipes.softDelete(id)) throw AppException.NotFound("Recipe not found")
    }

    private suspend fun assertCanModify(id: UUID, callerId: UUID, callerRole: UserRole) {
        val recipe = recipes.findById(id) ?: throw AppException.NotFound("Recipe not found")
        if (recipe.createdBy != callerId && callerRole != UserRole.ADMIN) {
            throw AppException.Forbidden("You can only modify your own recipes")
        }
    }

    /** Validates the request and resolves it into repository-layer data. */
    private suspend fun toData(request: RecipeRequest): RecipeData {
        val difficulty = runCatching { Difficulty.valueOf(request.difficulty.uppercase()) }.getOrNull()
        validate {
            require(request.title.trim().length in 3..200, "title", "Title must be 3–200 characters")
            require(request.cookingTimeMinutes > 0, "cookingTimeMinutes", "Must be greater than 0")
            require(request.servings > 0, "servings", "Must be greater than 0")
            require(difficulty != null, "difficulty", "Must be one of EASY, MEDIUM, HARD")
            require(request.instructions.isNotEmpty(), "instructions", "At least one step is required")
            require(request.instructions.all { it.isNotBlank() }, "instructions", "Steps must not be blank")
            require(request.ingredients.isNotEmpty(), "ingredients", "At least one ingredient is required")
            require(request.ingredients.all { it.quantity > 0 }, "ingredients", "Quantities must be greater than 0")
        }

        val ids = request.ingredients.map {
            runCatching { UUID.fromString(it.ingredientId) }
                .getOrElse { _ -> throw AppException.Validation(fields = mapOf("ingredients" to "Invalid ingredient id")) }
        }
        if (ids.size != ids.toSet().size) {
            throw AppException.Validation(fields = mapOf("ingredients" to "Duplicate ingredients are not allowed"))
        }
        val existing = ingredients.existingIds(ids)
        val missing = ids.filterNot { it in existing }
        if (missing.isNotEmpty()) {
            throw AppException.Validation(fields = mapOf("ingredients" to "Unknown ingredient(s): ${missing.joinToString()}"))
        }

        return RecipeData(
            title = request.title.trim(),
            description = request.description.trim(),
            cookingTimeMinutes = request.cookingTimeMinutes,
            servings = request.servings,
            difficulty = difficulty!!,
            coverImageUrl = request.coverImageUrl,
            instructions = request.instructions.map { it.trim() },
            ingredients = request.ingredients.mapIndexed { index, line ->
                RecipeIngredientData(
                    ingredientId = ids[index],
                    quantity = BigDecimal.valueOf(line.quantity),
                    unit = line.unit,
                    note = line.note?.trim()?.takeIf { it.isNotEmpty() },
                )
            },
        )
    }
}
