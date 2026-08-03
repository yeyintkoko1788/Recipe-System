package com.yeyint.recipeapp.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

const val RATE_LIMIT_AUTH = "auth"

/** Brute-force protection for the authentication endpoints. */
fun Application.configureRateLimiting() {
    install(RateLimit) {
        register(RateLimitName(RATE_LIMIT_AUTH)) {
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
            requestKey { call -> call.request.local.remoteHost }
        }
    }
}
