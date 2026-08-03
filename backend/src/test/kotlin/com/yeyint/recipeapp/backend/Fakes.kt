package com.yeyint.recipeapp.backend

import com.yeyint.recipeapp.backend.domain.Ingredient
import com.yeyint.recipeapp.backend.domain.IngredientStatus
import com.yeyint.recipeapp.backend.domain.Page
import com.yeyint.recipeapp.backend.domain.PantryItem
import com.yeyint.recipeapp.backend.domain.ShoppingItem
import com.yeyint.recipeapp.backend.domain.ShoppingSource
import com.yeyint.recipeapp.backend.domain.User
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.repository.IngredientRepository
import com.yeyint.recipeapp.backend.repository.PantryRepository
import com.yeyint.recipeapp.backend.repository.RefreshTokenRepository
import com.yeyint.recipeapp.backend.repository.ShoppingRepository
import com.yeyint.recipeapp.backend.repository.UserRepository
import com.yeyint.recipeapp.backend.security.PasswordHasher
import com.yeyint.recipeapp.backend.util.PageRequest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** In-memory fakes used by the service-layer unit tests. */

class FakePasswordHasher : PasswordHasher {
    override fun hash(raw: String) = "hashed:$raw"
    override fun verify(raw: String, hash: String) = hash == "hashed:$raw"
}

class FakeUserRepository : UserRepository {
    val storage = mutableMapOf<UUID, User>()

    override suspend fun findByEmail(email: String) =
        storage.values.firstOrNull { it.email.equals(email, ignoreCase = true) }

    override suspend fun findById(id: UUID) = storage[id]

    override suspend fun create(name: String, email: String, passwordHash: String, role: UserRole): User {
        val user = User(UUID.randomUUID(), name, email, passwordHash, role, Instant.now(), Instant.now())
        storage[user.id] = user
        return user
    }

    override suspend fun list(page: PageRequest, search: String?) =
        Page(storage.values.toList(), page.page, page.size, storage.size.toLong())

    override suspend fun updateRole(id: UUID, role: UserRole): Boolean {
        val user = storage[id] ?: return false
        storage[id] = user.copy(role = role)
        return true
    }

    override suspend fun delete(id: UUID) = storage.remove(id) != null
}

class FakeRefreshTokenRepository : RefreshTokenRepository {
    private data class Entry(val userId: UUID, val expiresAt: Instant, var revoked: Boolean)
    private val tokens = mutableMapOf<String, Entry>()

    override suspend fun store(userId: UUID, tokenHash: String, expiresAt: Instant) {
        tokens[tokenHash] = Entry(userId, expiresAt, revoked = false)
    }

    override suspend fun findUserIdByValidToken(tokenHash: String, now: Instant): UUID? =
        tokens[tokenHash]?.takeIf { !it.revoked && it.expiresAt.isAfter(now) }?.userId

    override suspend fun revoke(tokenHash: String): Boolean {
        val entry = tokens[tokenHash] ?: return false
        entry.revoked = true
        return true
    }

    override suspend fun revokeAllForUser(userId: UUID) {
        tokens.values.filter { it.userId == userId }.forEach { it.revoked = true }
    }
}

class FakeIngredientRepository : IngredientRepository {
    val storage = mutableMapOf<UUID, Ingredient>()

    fun seed(name: String, status: IngredientStatus = IngredientStatus.APPROVED): Ingredient {
        val ingredient = Ingredient(
            UUID.randomUUID(), name, "OTHER", "g", null, status, null, Instant.now(), Instant.now(),
        )
        storage[ingredient.id] = ingredient
        return ingredient
    }

    override suspend fun findById(id: UUID) = storage[id]

    override suspend fun findByName(name: String) =
        storage.values.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    override suspend fun existingIds(ids: Collection<UUID>) =
        ids.filter { it in storage }.toSet()

    override suspend fun create(
        name: String, category: String, defaultUnit: String,
        imageUrl: String?, status: IngredientStatus, createdBy: UUID?,
    ): Ingredient {
        val ingredient = Ingredient(
            UUID.randomUUID(), name.trim(), category, defaultUnit, imageUrl, status, createdBy,
            Instant.now(), Instant.now(),
        )
        storage[ingredient.id] = ingredient
        return ingredient
    }

    override suspend fun update(id: UUID, name: String, category: String, defaultUnit: String, imageUrl: String?): Ingredient? {
        val existing = storage[id] ?: return null
        val updated = existing.copy(name = name.trim(), category = category, defaultUnit = defaultUnit, imageUrl = imageUrl)
        storage[id] = updated
        return updated
    }

    override suspend fun updateStatus(id: UUID, status: IngredientStatus): Boolean {
        val existing = storage[id] ?: return false
        storage[id] = existing.copy(status = status)
        return true
    }

    override suspend fun delete(id: UUID) = storage.remove(id) != null

    override suspend fun list(page: PageRequest, search: String?, category: String?, status: IngredientStatus?) =
        Page(storage.values.toList(), page.page, page.size, storage.size.toLong())
}

class FakePantryRepository : PantryRepository {
    val storage = mutableMapOf<UUID, PantryItem>()

    fun seed(userId: UUID, ingredient: Ingredient, quantity: Double, outOfStock: Boolean = false): PantryItem {
        val item = PantryItem(
            UUID.randomUUID(), userId, ingredient, BigDecimal.valueOf(quantity),
            ingredient.defaultUnit, outOfStock, Instant.now(),
        )
        storage[item.id] = item
        return item
    }

    override suspend fun listForUser(userId: UUID) = storage.values.filter { it.userId == userId }

    override suspend fun find(userId: UUID, id: UUID) = storage[id]?.takeIf { it.userId == userId }

    override suspend fun findByIngredient(userId: UUID, ingredientId: UUID) =
        storage.values.firstOrNull { it.userId == userId && it.ingredient.id == ingredientId }

    override suspend fun upsert(userId: UUID, ingredientId: UUID, quantity: BigDecimal, unit: String): PantryItem {
        error("Not used in these tests")
    }

    override suspend fun update(
        userId: UUID, id: UUID, quantity: BigDecimal?, unit: String?, isOutOfStock: Boolean?,
    ): PantryItem? {
        val existing = find(userId, id) ?: return null
        val updated = existing.copy(
            quantity = quantity ?: existing.quantity,
            unit = unit ?: existing.unit,
            isOutOfStock = isOutOfStock ?: existing.isOutOfStock,
            updatedAt = Instant.now(),
        )
        storage[id] = updated
        return updated
    }

    override suspend fun delete(userId: UUID, id: UUID) =
        find(userId, id)?.let { storage.remove(id); true } ?: false

    override suspend fun outOfStockItems(userId: UUID) =
        storage.values.filter { it.userId == userId && it.isOutOfStock }
}

class FakeShoppingRepository : ShoppingRepository {
    val storage = mutableMapOf<UUID, ShoppingItem>()

    override suspend fun listForUser(userId: UUID, includePurchased: Boolean) =
        storage.values.filter { it.userId == userId && (includePurchased || !it.isPurchased) }

    override suspend fun find(userId: UUID, id: UUID) = storage[id]?.takeIf { it.userId == userId }

    override suspend fun add(
        userId: UUID, ingredientId: UUID?, name: String,
        quantity: BigDecimal?, unit: String?, source: ShoppingSource,
    ): ShoppingItem {
        val item = ShoppingItem(
            UUID.randomUUID(), userId, ingredientId, name, quantity, unit, source,
            isPurchased = false, createdAt = Instant.now(), purchasedAt = null,
        )
        storage[item.id] = item
        return item
    }

    override suspend fun existsUnpurchasedForIngredient(userId: UUID, ingredientId: UUID) =
        storage.values.any { it.userId == userId && it.ingredientId == ingredientId && !it.isPurchased }

    override suspend fun markPurchased(userId: UUID, id: UUID): ShoppingItem? {
        val existing = find(userId, id) ?: return null
        val updated = existing.copy(isPurchased = true, purchasedAt = Instant.now())
        storage[id] = updated
        return updated
    }

    override suspend fun delete(userId: UUID, id: UUID) =
        find(userId, id)?.let { storage.remove(id); true } ?: false

    override suspend fun clearPurchased(userId: UUID): Int {
        val purchased = storage.values.filter { it.userId == userId && it.isPurchased }
        purchased.forEach { storage.remove(it.id) }
        return purchased.size
    }
}
