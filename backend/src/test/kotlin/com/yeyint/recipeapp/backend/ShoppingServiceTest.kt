package com.yeyint.recipeapp.backend

import com.yeyint.recipeapp.backend.domain.ShoppingSource
import com.yeyint.recipeapp.backend.dto.PurchaseRequest
import com.yeyint.recipeapp.backend.service.ShoppingService
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShoppingServiceTest {

    private val ingredients = FakeIngredientRepository()
    private val pantry = FakePantryRepository()
    private val shopping = FakeShoppingRepository()
    private val service = ShoppingService(shopping, pantry)
    private val userId = UUID.randomUUID()

    @Test
    fun `generate creates items only for out-of-stock pantry entries`() = runTest {
        val salt = ingredients.seed("Salt")
        val rice = ingredients.seed("Rice")
        val oil = ingredients.seed("Oil")
        pantry.seed(userId, salt, quantity = 0.0, outOfStock = true)
        pantry.seed(userId, rice, quantity = 0.0, outOfStock = true)
        pantry.seed(userId, oil, quantity = 500.0, outOfStock = false)

        val generated = service.generateFromPantry(userId)

        assertEquals(setOf("Salt", "Rice"), generated.map { it.name }.toSet())
        assertTrue(generated.all { it.source == ShoppingSource.PANTRY })
    }

    @Test
    fun `generate does not duplicate items already on the list`() = runTest {
        val salt = ingredients.seed("Salt")
        pantry.seed(userId, salt, quantity = 0.0, outOfStock = true)

        val first = service.generateFromPantry(userId)
        val second = service.generateFromPantry(userId)

        assertEquals(1, first.size)
        assertTrue(second.isEmpty(), "Second generation should add nothing")
    }

    @Test
    fun `purchasing a pantry-linked item restores pantry stock`() = runTest {
        val salt = ingredients.seed("Salt")
        val pantryItem = pantry.seed(userId, salt, quantity = 0.0, outOfStock = true)
        val listItem = service.generateFromPantry(userId).single()

        service.purchase(userId, listItem.id, PurchaseRequest(restockPantry = true, quantity = 250.0))

        val restocked = pantry.find(userId, pantryItem.id)!!
        assertFalse(restocked.isOutOfStock)
        assertEquals(0, restocked.quantity.compareTo(java.math.BigDecimal.valueOf(250.0)))
        assertTrue(shopping.storage.getValue(listItem.id).isPurchased)
    }
}
