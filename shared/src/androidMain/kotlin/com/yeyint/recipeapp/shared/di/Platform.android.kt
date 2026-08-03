package com.yeyint.recipeapp.shared.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.yeyint.recipeapp.shared.db.RecipeDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android platform bindings. [AppContextHolder] is initialised by the
 * `Application` class before Koin starts.
 */
object AppContextHolder {
    lateinit var context: Context
}

actual class DatabaseDriverFactory(private val context: Context = AppContextHolder.context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(RecipeDatabase.Schema, context, "recipeapp.db")
}

actual fun createSettings(name: String): Settings =
    SharedPreferencesSettings(
        AppContextHolder.context.getSharedPreferences(name, Context.MODE_PRIVATE),
    )

actual fun httpClientEngine(): HttpClientEngine = OkHttp.create()

val androidPlatformModule = org.koin.dsl.module {
    single { DatabaseDriverFactory() }
}
