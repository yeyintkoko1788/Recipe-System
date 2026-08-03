package com.yeyint.recipeapp.shared.di

import com.yeyint.recipeapp.shared.data.local.RecipeCacheDataSource
import com.yeyint.recipeapp.shared.data.local.TokenStorage
import com.yeyint.recipeapp.shared.data.remote.ApiConfig
import com.yeyint.recipeapp.shared.data.remote.api.AuthApi
import com.yeyint.recipeapp.shared.data.remote.api.CatalogApi
import com.yeyint.recipeapp.shared.data.remote.api.RecipeApi
import com.yeyint.recipeapp.shared.data.remote.createHttpClient
import com.yeyint.recipeapp.shared.data.repository.AuthRepositoryImpl
import com.yeyint.recipeapp.shared.data.repository.ExploreRepositoryImpl
import com.yeyint.recipeapp.shared.data.repository.IngredientRepositoryImpl
import com.yeyint.recipeapp.shared.data.repository.PantryRepositoryImpl
import com.yeyint.recipeapp.shared.data.repository.RecipeRepositoryImpl
import com.yeyint.recipeapp.shared.data.repository.SettingsRepositoryImpl
import com.yeyint.recipeapp.shared.data.repository.ShoppingRepositoryImpl
import com.yeyint.recipeapp.shared.db.RecipeDatabase
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.domain.repository.ExploreRepository
import com.yeyint.recipeapp.shared.domain.repository.IngredientRepository
import com.yeyint.recipeapp.shared.domain.repository.PantryRepository
import com.yeyint.recipeapp.shared.domain.repository.RecipeRepository
import com.yeyint.recipeapp.shared.domain.repository.SettingsRepository
import com.yeyint.recipeapp.shared.domain.repository.ShoppingRepository
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin modules for everything platform-independent. The mobile entry points
 * call [initSharedKoin] (Android from `Application`, iOS from `MainViewController`).
 */
fun sharedModule(config: ApiConfig): Module = module {
    single { config }
    single { TokenStorage(createSettings("recipeapp_auth")) }
    single<SettingsRepository> { SettingsRepositoryImpl(createSettings("recipeapp_settings")) }

    single { RecipeDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { RecipeCacheDataSource(get()) }

    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    single {
        createHttpClient(
            engine = httpClientEngine(),
            config = get(),
            tokenStorage = get(),
            onSessionExpired = { get<AuthRepository>().onSessionExpired() },
        )
    }

    single { AuthApi(get()) }
    single { RecipeApi(get()) }
    single { CatalogApi(get()) }

    single<RecipeRepository> { RecipeRepositoryImpl(get()) }
    single<ExploreRepository> { ExploreRepositoryImpl(get(), get()) }
    single<IngredientRepository> { IngredientRepositoryImpl(get()) }
    single<PantryRepository> { PantryRepositoryImpl(get()) }
    single<ShoppingRepository> { ShoppingRepositoryImpl(get()) }
}

fun initSharedKoin(
    config: ApiConfig,
    platformModule: Module,
    extraModules: List<Module> = emptyList(),
): KoinApplication = startKoin {
    modules(listOf(platformModule, sharedModule(config)) + extraModules)
}
