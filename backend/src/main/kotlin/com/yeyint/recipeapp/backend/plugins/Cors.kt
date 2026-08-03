package com.yeyint.recipeapp.backend.plugins

import com.yeyint.recipeapp.backend.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(config: AppConfig) {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Request-Id")
        if (config.cors.allowedHosts.contains("*")) {
            anyHost() // Development only — restrict via CORS_ALLOWED_HOSTS in production.
        } else {
            config.cors.allowedHosts.forEach { allowHost(it, schemes = listOf("http", "https")) }
        }
    }
}
