package com.yeyint.recipeapp.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.yeyint.recipeapp.shared.db.RecipeDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSUserDefaults

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(RecipeDatabase.Schema, "recipeapp.db")
}

actual fun createSettings(name: String): Settings =
    NSUserDefaultsSettings(NSUserDefaults(suiteName = name))

actual fun httpClientEngine(): HttpClientEngine = Darwin.create()

val iosPlatformModule = org.koin.dsl.module {
    single { DatabaseDriverFactory() }
}
