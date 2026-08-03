package com.yeyint.recipeapp.di

import com.yeyint.recipeapp.feature.auth.LoginViewModel
import com.yeyint.recipeapp.feature.auth.RegisterViewModel
import com.yeyint.recipeapp.feature.explore.ExploreViewModel
import com.yeyint.recipeapp.feature.home.HomeViewModel
import com.yeyint.recipeapp.feature.ingredients.IngredientListViewModel
import com.yeyint.recipeapp.feature.pantry.PantryViewModel
import com.yeyint.recipeapp.feature.profile.ProfileViewModel
import com.yeyint.recipeapp.feature.recipedetail.RecipeDetailViewModel
import com.yeyint.recipeapp.feature.recipeeditor.RecipeEditorViewModel
import com.yeyint.recipeapp.feature.search.SearchViewModel
import com.yeyint.recipeapp.feature.settings.SettingsViewModel
import com.yeyint.recipeapp.feature.shopping.ShoppingListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** ViewModel definitions for the presentation layer. */
val appModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { ExploreViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { RecipeDetailViewModel(get(), get()) }
    viewModel { RecipeEditorViewModel(get(), get()) }
    viewModel { IngredientListViewModel(get()) }
    viewModel { PantryViewModel(get()) }
    viewModel { ShoppingListViewModel(get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
