package com.yeyint.recipeapp.backend.service

import com.yeyint.recipeapp.backend.domain.Ingredient
import com.yeyint.recipeapp.backend.domain.IngredientStatus
import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.dto.IngredientRequest
import com.yeyint.recipeapp.backend.repository.IngredientRepository
import com.yeyint.recipeapp.backend.util.AppException
import com.yeyint.recipeapp.backend.util.PageRequest
import com.yeyint.recipeapp.backend.util.validate
import java.util.UUID

/**
 * Global ingredient catalog.
 *
 * Uniqueness is case-insensitive ("Salt" == "salt" == "SALT") and enforced
 * twice: here for a friendly 409, and by a functional unique index in
 * PostgreSQL as the authoritative guard against race conditions.
 */
class IngredientService(private val ingredients: IngredientRepository) {

    suspend fun list(
        page: PageRequest, search: String?, category: String?,
        status: String?, callerRole: UserRole,
    ): Page<Ingredient> {
        // Only admins may browse non-approved entries.
        val effectiveStatus = when {
            callerRole == UserRole.ADMIN && status != null ->
                runCatching { IngredientStatus.valueOf(status.uppercase()) }
                    .getOrElse { throw AppException.Validation(fields = mapOf("status" to "Unknown status")) }
            callerRole == UserRole.ADMIN -> null
            else -> IngredientStatus.APPROVED
        }
        return ingredients.list(page, search, category, effectiveStatus)
    }

    suspend fun get(id: UUID): Ingredient =
        ingredients.findById(id) ?: throw AppException.NotFound("Ingredient not found")

    /**
     * Admin-created ingredients are APPROVED immediately; user submissions
     * start PENDING so the catalog stays curated.
     */
    suspend fun create(request: IngredientRequest, callerId: UUID, callerRole: UserRole): Ingredient {
        validateRequest(request)
        ensureNameAvailable(request.name)
        val status = if (callerRole == UserRole.ADMIN) IngredientStatus.APPROVED else IngredientStatus.PENDING
        return ingredients.create(
            name = request.name, category = request.category.uppercase(),
            defaultUnit = request.defaultUnit, imageUrl = request.imageUrl,
            status = status, createdBy = callerId,
        )
    }

    suspend fun update(id: UUID, request: IngredientRequest): Ingredient {
        validateRequest(request)
        ensureNameAvailable(request.name, exceptId = id)
        return ingredients.update(id, request.name, request.category.uppercase(), request.defaultUnit, request.imageUrl)
            ?: throw AppException.NotFound("Ingredient not found")
    }

    suspend fun moderate(id: UUID, status: String): Ingredient {
        val parsed = runCatching { IngredientStatus.valueOf(status.uppercase()) }
            .getOrElse { throw AppException.Validation(fields = mapOf("status" to "Unknown status")) }
        if (!ingredients.updateStatus(id, parsed)) throw AppException.NotFound("Ingredient not found")
        return get(id)
    }

    suspend fun delete(id: UUID) {
        val deleted = runCatching { ingredients.delete(id) }
            .getOrElse {
                // FK RESTRICT from recipe_ingredients: ingredient is in use.
                throw AppException.Conflict("Ingredient is used by existing recipes and cannot be deleted")
            }
        if (!deleted) throw AppException.NotFound("Ingredient not found")
    }

    private fun validateRequest(request: IngredientRequest) = validate {
        require(request.name.trim().length in 2..100, "name", "Name must be 2–100 characters")
        require(request.category.isNotBlank(), "category", "Category is required")
        require(request.defaultUnit.isNotBlank(), "defaultUnit", "Default unit is required")
    }

    private suspend fun ensureNameAvailable(name: String, exceptId: UUID? = null) {
        val existing = ingredients.findByName(name) ?: return
        if (existing.id != exceptId) {
            throw AppException.Conflict("Ingredient '${existing.name}' already exists")
        }
    }
}
