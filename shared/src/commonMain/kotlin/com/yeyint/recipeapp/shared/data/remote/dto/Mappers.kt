package com.yeyint.recipeapp.shared.data.remote.dto

import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.Ingredient
import com.yeyint.recipeapp.shared.domain.model.IngredientStatus
import com.yeyint.recipeapp.shared.domain.model.Page
import com.yeyint.recipeapp.shared.domain.model.PantryItem
import com.yeyint.recipeapp.shared.domain.model.RecipeDetail
import com.yeyint.recipeapp.shared.domain.model.RecipeDraft
import com.yeyint.recipeapp.shared.domain.model.RecipeIngredientLine
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.model.ShoppingItem
import com.yeyint.recipeapp.shared.domain.model.ShoppingSource
import com.yeyint.recipeapp.shared.domain.model.User
import com.yeyint.recipeapp.shared.domain.model.UserRole

/** DTO → domain mappers. Unknown enum values degrade gracefully. */

private inline fun <reified E : Enum<E>> String.toEnumOr(default: E): E =
    enumValues<E>().firstOrNull { it.name == this.uppercase() } ?: default

fun UserDto.toDomain() = User(id, name, email, role.toEnumOr(UserRole.USER))

fun IngredientDto.toDomain() = Ingredient(
    id, name, category, defaultUnit, imageUrl, status.toEnumOr(IngredientStatus.APPROVED),
)

fun RecipeDto.toSummary() = RecipeSummary(
    id = id,
    title = title,
    description = description,
    cookingTimeMinutes = cookingTimeMinutes,
    servings = servings,
    difficulty = difficulty.toEnumOr(Difficulty.MEDIUM),
    coverImageUrl = coverImageUrl,
    authorName = authorName,
    viewCount = viewCount,
    createdAt = createdAt,
)

fun RecipeDto.toDetail() = RecipeDetail(
    id = id,
    title = title,
    description = description,
    cookingTimeMinutes = cookingTimeMinutes,
    servings = servings,
    difficulty = difficulty.toEnumOr(Difficulty.MEDIUM),
    coverImageUrl = coverImageUrl,
    instructions = instructions,
    ingredients = ingredients.map {
        RecipeIngredientLine(it.ingredientId, it.ingredientName, it.quantity, it.unit, it.note)
    },
    authorId = authorId,
    authorName = authorName,
    viewCount = viewCount,
)

fun RecipeDraft.toRequest() = RecipeRequestDto(
    title = title,
    description = description,
    cookingTimeMinutes = cookingTimeMinutes,
    servings = servings,
    difficulty = difficulty.name,
    coverImageUrl = coverImageUrl,
    instructions = instructions,
    ingredients = ingredients.map {
        RecipeIngredientRequestDto(it.ingredientId, it.quantity, it.unit, it.note)
    },
)

fun PantryItemDto.toDomain() = PantryItem(
    id, ingredient.toDomain(), quantity, unit, isOutOfStock,
)

fun ShoppingItemDto.toDomain() = ShoppingItem(
    id, ingredientId, name, quantity, unit, source.toEnumOr(ShoppingSource.MANUAL), isPurchased,
)

fun <T, R> PageDto<T>.toDomain(transform: (T) -> R) =
    Page(items.map(transform), page, size, totalItems, totalPages)
