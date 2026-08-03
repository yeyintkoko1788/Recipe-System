package com.yeyint.recipeapp.backend

import com.yeyint.recipeapp.backend.domain.IngredientStatus
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.dto.IngredientRequest
import com.yeyint.recipeapp.backend.service.IngredientService
import com.yeyint.recipeapp.backend.util.AppException
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IngredientServiceTest {

    private val repository = FakeIngredientRepository()
    private val service = IngredientService(repository)
    private val adminId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @Test
    fun `duplicate names are rejected case-insensitively`() = runTest {
        service.create(IngredientRequest("Salt", "SPICE", "g"), adminId, UserRole.ADMIN)

        // "salt" and "SALT" must be treated as the same ingredient.
        assertFailsWith<AppException.Conflict> {
            service.create(IngredientRequest("salt", "SPICE", "g"), adminId, UserRole.ADMIN)
        }
        assertFailsWith<AppException.Conflict> {
            service.create(IngredientRequest("SALT", "SPICE", "g"), userId, UserRole.USER)
        }
        assertFailsWith<AppException.Conflict> {
            service.create(IngredientRequest("  Salt  ", "SPICE", "g"), adminId, UserRole.ADMIN)
        }
    }

    @Test
    fun `admin submissions are approved immediately`() = runTest {
        val created = service.create(IngredientRequest("Basil", "HERB", "g"), adminId, UserRole.ADMIN)
        assertEquals(IngredientStatus.APPROVED, created.status)
    }

    @Test
    fun `user submissions start as pending`() = runTest {
        val created = service.create(IngredientRequest("Lemongrass", "HERB", "g"), userId, UserRole.USER)
        assertEquals(IngredientStatus.PENDING, created.status)
    }

    @Test
    fun `update may keep its own name but not steal another's`() = runTest {
        val salt = service.create(IngredientRequest("Salt", "SPICE", "g"), adminId, UserRole.ADMIN)
        service.create(IngredientRequest("Sugar", "BAKING", "g"), adminId, UserRole.ADMIN)

        // Renaming Salt to "salt" (same ingredient) is fine…
        service.update(salt.id, IngredientRequest("salt", "SPICE", "g"))
        // …but renaming it to "sugar" collides.
        assertFailsWith<AppException.Conflict> {
            service.update(salt.id, IngredientRequest("sugar", "SPICE", "g"))
        }
    }
}
