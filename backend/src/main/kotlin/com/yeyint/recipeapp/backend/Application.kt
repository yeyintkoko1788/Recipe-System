package com.yeyint.recipeapp.backend

import com.yeyint.recipeapp.backend.config.AppConfig
import com.yeyint.recipeapp.backend.config.DatabaseFactory
import com.yeyint.recipeapp.backend.di.Dependencies
import com.yeyint.recipeapp.backend.plugins.configureCors
import com.yeyint.recipeapp.backend.plugins.configureMonitoring
import com.yeyint.recipeapp.backend.plugins.configureRateLimiting
import com.yeyint.recipeapp.backend.plugins.configureRouting
import com.yeyint.recipeapp.backend.plugins.configureSecurity
import com.yeyint.recipeapp.backend.plugins.configureSerialization
import com.yeyint.recipeapp.backend.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): Unit = EngineMain.main(args)

/**
 * Application entry point wired from `resources/application.conf`.
 *
 * Startup order matters: configuration → database (incl. Flyway migrations) →
 * dependency graph → Ktor plugins → routes → bootstrap data.
 */
fun Application.module() {
    val config = AppConfig.from(environment.config)
    DatabaseFactory.init(config.database)

    val deps = Dependencies(config)

    configureSerialization()
    configureMonitoring()
    configureCors(config)
    configureSecurity(config)
    configureRateLimiting()
    configureStatusPages()
    configureRouting(deps)

    // Ensure the bootstrap admin account exists (idempotent).
    runBlocking { deps.authService.ensureAdminUser(config.admin) }
}
