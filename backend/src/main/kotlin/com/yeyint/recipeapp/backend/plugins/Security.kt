package com.yeyint.recipeapp.backend.plugins

import com.yeyint.recipeapp.backend.config.AppConfig
import com.yeyint.recipeapp.backend.security.JwtService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

const val AUTH_JWT = "auth-jwt"

/**
 * JWT bearer authentication. Authorization (role checks) is enforced per-route
 * via `call.requireAdmin()` so the security config stays declarative.
 */
fun Application.configureSecurity(config: AppConfig) {
    val jwtService = JwtService(config.jwt)
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = jwtService.realm
            verifier(jwtService.verifier)
            validate { credential ->
                if (credential.payload.subject.isNullOrBlank()) null else JWTPrincipal(credential.payload)
            }
        }
    }
}
