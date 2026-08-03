package com.yeyint.recipeapp.backend.config

import io.ktor.server.config.ApplicationConfig

/**
 * Typed, immutable view over `application.conf`. Every value can be overridden
 * with environment variables (see the conf file), which is how Docker and
 * production deployments configure the app — code never reads env vars
 * directly.
 */
data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val admin: AdminConfig,
    val cors: CorsConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig = AppConfig(
            database = DatabaseConfig(
                url = config.property("app.database.url").getString(),
                user = config.property("app.database.user").getString(),
                password = config.property("app.database.password").getString(),
                maxPoolSize = config.property("app.database.maxPoolSize").getString().toInt(),
                migrationsLocation = config.property("app.database.migrationsLocation").getString(),
            ),
            jwt = JwtConfig(
                secret = config.property("app.jwt.secret").getString(),
                issuer = config.property("app.jwt.issuer").getString(),
                audience = config.property("app.jwt.audience").getString(),
                accessTtlMinutes = config.property("app.jwt.accessTtlMinutes").getString().toLong(),
                refreshTtlDays = config.property("app.jwt.refreshTtlDays").getString().toLong(),
            ),
            admin = AdminConfig(
                email = config.property("app.admin.email").getString(),
                password = config.property("app.admin.password").getString(),
                name = config.property("app.admin.name").getString(),
            ),
            cors = CorsConfig(
                allowedHosts = config.property("app.cors.allowedHosts").getString()
                    .split(',').map { it.trim() }.filter { it.isNotEmpty() },
            ),
        )
    }
}

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
    val migrationsLocation: String,
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTtlMinutes: Long,
    val refreshTtlDays: Long,
)

data class AdminConfig(val email: String, val password: String, val name: String)

data class CorsConfig(val allowedHosts: List<String>)
