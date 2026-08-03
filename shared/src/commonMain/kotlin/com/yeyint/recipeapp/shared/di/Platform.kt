package com.yeyint.recipeapp.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine

/** Platform-provided pieces, implemented in androidMain / iosMain. */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

expect fun createSettings(name: String): Settings

expect fun httpClientEngine(): HttpClientEngine
