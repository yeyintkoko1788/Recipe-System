package com.yeyint.recipeapp.backend.db

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

/**
 * Exposed DSL table definitions. These mirror the Flyway migrations exactly —
 * Flyway owns the schema, Exposed only reads/writes it.
 */

object UsersTable : UUIDTable("users") {
    val name = varchar("name", 100)
    val email = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 16)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = char("token_hash", 64).uniqueIndex()
    val expiresAt = timestampWithTimeZone("expires_at")
    val revoked = bool("revoked").default(false)
    val createdAt = timestampWithTimeZone("created_at")
}

object IngredientsTable : UUIDTable("ingredients") {
    val name = varchar("name", 100)
    val category = varchar("category", 50)
    val defaultUnit = varchar("default_unit", 20)
    val imageUrl = text("image_url").nullable()
    val status = varchar("status", 16)
    val createdBy = reference("created_by", UsersTable).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object RecipesTable : UUIDTable("recipes") {
    val title = varchar("title", 200)
    val description = text("description")
    val cookingTimeMinutes = integer("cooking_time_minutes")
    val servings = integer("servings")
    val difficulty = varchar("difficulty", 16)
    val coverImageUrl = text("cover_image_url").nullable()
    val instructions = jsonb<List<String>>("instructions", Json.Default)
    val createdBy = reference("created_by", UsersTable)
    val viewCount = long("view_count").default(0)
    val isDeleted = bool("is_deleted").default(false)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object RecipeIngredientsTable : Table("recipe_ingredients") {
    val recipeId = reference("recipe_id", RecipesTable)
    val ingredientId = reference("ingredient_id", IngredientsTable)
    val quantity = decimal("quantity", 10, 2)
    val unit = varchar("unit", 20)
    val note = varchar("note", 255).nullable()
    override val primaryKey = PrimaryKey(recipeId, ingredientId)
}

object PantryItemsTable : UUIDTable("pantry_items") {
    val userId = reference("user_id", UsersTable)
    val ingredientId = reference("ingredient_id", IngredientsTable)
    val quantity = decimal("quantity", 10, 2)
    val unit = varchar("unit", 20)
    val isOutOfStock = bool("is_out_of_stock").default(false)
    val updatedAt = timestampWithTimeZone("updated_at")

    init {
        uniqueIndex(userId, ingredientId)
    }
}

object ShoppingItemsTable : UUIDTable("shopping_items") {
    val userId = reference("user_id", UsersTable)
    val ingredientId = reference("ingredient_id", IngredientsTable).nullable()
    val name = varchar("name", 100)
    val quantity = decimal("quantity", 10, 2).nullable()
    val unit = varchar("unit", 20).nullable()
    val itemSource = varchar("source", 16)
    val isPurchased = bool("is_purchased").default(false)
    val createdAt = timestampWithTimeZone("created_at")
    val purchasedAt = timestampWithTimeZone("purchased_at").nullable()
}
