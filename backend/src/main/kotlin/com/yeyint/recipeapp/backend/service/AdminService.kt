package com.yeyint.recipeapp.backend.service

import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.User
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.dto.StatsResponse
import com.yeyint.recipeapp.backend.repository.IngredientRepository
import com.yeyint.recipeapp.backend.repository.RecipeRepository
import com.yeyint.recipeapp.backend.repository.StatsRepository
import com.yeyint.recipeapp.backend.repository.UserRepository
import com.yeyint.recipeapp.backend.util.AppException
import com.yeyint.recipeapp.backend.util.PageRequest
import java.util.UUID

/** Admin-only operations: user management and dashboard statistics. */
class AdminService(
    private val users: UserRepository,
    @Suppress("unused") private val recipes: RecipeRepository,
    @Suppress("unused") private val ingredients: IngredientRepository,
    private val stats: StatsRepository,
) {

    suspend fun listUsers(page: PageRequest, search: String?): Page<User> = users.list(page, search)

    suspend fun updateUserRole(actorId: UUID, targetId: UUID, role: String): User {
        if (actorId == targetId) throw AppException.Forbidden("You cannot change your own role")
        val parsed = runCatching { UserRole.valueOf(role.uppercase()) }
            .getOrElse { throw AppException.Validation(fields = mapOf("role" to "Must be ADMIN or USER")) }
        if (!users.updateRole(targetId, parsed)) throw AppException.NotFound("User not found")
        return users.findById(targetId) ?: throw AppException.NotFound("User not found")
    }

    suspend fun deleteUser(actorId: UUID, targetId: UUID) {
        if (actorId == targetId) throw AppException.Forbidden("You cannot delete your own account here")
        if (!users.delete(targetId)) throw AppException.NotFound("User not found")
    }

    suspend fun statistics(): StatsResponse = stats.snapshot().let {
        StatsResponse(
            totalUsers = it.totalUsers,
            totalRecipes = it.totalRecipes,
            totalIngredients = it.totalIngredients,
            pendingIngredients = it.pendingIngredients,
            recipesCreatedLast7Days = it.recipesCreatedLast7Days,
        )
    }
}
