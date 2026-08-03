package com.yeyint.recipeapp.backend.di

import com.yeyint.recipeapp.backend.config.AppConfig
import com.yeyint.recipeapp.backend.repository.exposed.ExposedIngredientRepository
import com.yeyint.recipeapp.backend.repository.exposed.ExposedPantryRepository
import com.yeyint.recipeapp.backend.repository.exposed.ExposedRecipeRepository
import com.yeyint.recipeapp.backend.repository.exposed.ExposedRefreshTokenRepository
import com.yeyint.recipeapp.backend.repository.exposed.ExposedShoppingRepository
import com.yeyint.recipeapp.backend.repository.exposed.ExposedStatsRepository
import com.yeyint.recipeapp.backend.repository.exposed.ExposedUserRepository
import com.yeyint.recipeapp.backend.security.BcryptPasswordHasher
import com.yeyint.recipeapp.backend.security.JwtService
import com.yeyint.recipeapp.backend.service.AdminService
import com.yeyint.recipeapp.backend.service.AuthService
import com.yeyint.recipeapp.backend.service.ExploreService
import com.yeyint.recipeapp.backend.service.IngredientService
import com.yeyint.recipeapp.backend.service.PantryService
import com.yeyint.recipeapp.backend.service.RecipeService
import com.yeyint.recipeapp.backend.service.ShoppingService

/**
 * Composition root: the whole object graph is wired here with plain
 * constructor injection. Explicit, reflection-free, and trivially replaceable
 * with fakes in tests. (The mobile apps use Koin; the server keeps DI manual
 * for fast startup and full compile-time safety.)
 */
class Dependencies(config: AppConfig) {

    // Infrastructure
    private val passwordHasher = BcryptPasswordHasher()
    private val jwtService = JwtService(config.jwt)

    // Repositories (Exposed-backed)
    val userRepository = ExposedUserRepository()
    val refreshTokenRepository = ExposedRefreshTokenRepository()
    val ingredientRepository = ExposedIngredientRepository()
    val recipeRepository = ExposedRecipeRepository()
    val pantryRepository = ExposedPantryRepository()
    val shoppingRepository = ExposedShoppingRepository()
    val statsRepository = ExposedStatsRepository()

    // Services
    val authService = AuthService(userRepository, refreshTokenRepository, passwordHasher, jwtService)
    val ingredientService = IngredientService(ingredientRepository)
    val recipeService = RecipeService(recipeRepository, ingredientRepository)
    val pantryService = PantryService(pantryRepository, ingredientRepository)
    val shoppingService = ShoppingService(shoppingRepository, pantryRepository)
    val exploreService = ExploreService(recipeRepository)
    val adminService = AdminService(userRepository, recipeRepository, ingredientRepository, statsRepository)
}
