package com.yeyint.recipeapp.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations (kotlinx.serialization based). */
sealed interface Route {

    // Auth graph
    @Serializable data object Login : Route
    @Serializable data object Register : Route

    // Main graph — bottom bar tabs
    @Serializable data object Home : Route
    @Serializable data object Explore : Route
    @Serializable data object Pantry : Route
    @Serializable data object Shopping : Route
    @Serializable data object Profile : Route

    // Main graph — pushed screens
    @Serializable data object Search : Route
    @Serializable data object Ingredients : Route
    @Serializable data object Settings : Route
    @Serializable data class RecipeDetail(val recipeId: String) : Route
    /** [recipeId] == null → create mode, otherwise edit mode. */
    @Serializable data class RecipeEditor(val recipeId: String? = null) : Route
}
