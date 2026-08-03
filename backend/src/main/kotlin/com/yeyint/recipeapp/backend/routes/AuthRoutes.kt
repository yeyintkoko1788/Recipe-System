package com.yeyint.recipeapp.backend.routes

import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.dto.LoginRequest
import com.yeyint.recipeapp.backend.dto.MessageResponse
import com.yeyint.recipeapp.backend.dto.RefreshTokenRequest
import com.yeyint.recipeapp.backend.dto.RegisterRequest
import com.yeyint.recipeapp.backend.dto.toResponse
import com.yeyint.recipeapp.backend.plugins.AUTH_JWT
import com.yeyint.recipeapp.backend.plugins.RATE_LIMIT_AUTH
import com.yeyint.recipeapp.backend.service.AuthService
import com.yeyint.recipeapp.backend.util.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(auth: AuthService) {
    route("/auth") {
        rateLimit(RateLimitName(RATE_LIMIT_AUTH)) {
            post("/register") {
                val response = auth.register(call.receive<RegisterRequest>())
                call.respond(HttpStatusCode.Created, ApiResponse.ok(response))
            }
            post("/login") {
                call.respond(ApiResponse.ok(auth.login(call.receive<LoginRequest>())))
            }
            post("/refresh") {
                val request = call.receive<RefreshTokenRequest>()
                call.respond(ApiResponse.ok(auth.refresh(request.refreshToken)))
            }
        }
        authenticate(AUTH_JWT) {
            post("/logout") {
                val request = call.receive<RefreshTokenRequest>()
                auth.logout(request.refreshToken)
                call.respond(ApiResponse.ok(MessageResponse("Logged out")))
            }
            get("/me") {
                call.respond(ApiResponse.ok(auth.me(call.userId()).toResponse()))
            }
        }
    }
}
