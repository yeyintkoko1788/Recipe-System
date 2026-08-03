package com.yeyint.recipeapp.backend.routes

import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.dto.toResponse
import com.yeyint.recipeapp.backend.plugins.AUTH_JWT
import com.yeyint.recipeapp.backend.service.ExploreService
import com.yeyint.recipeapp.backend.util.userId
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.exploreRoutes(service: ExploreService) {
    authenticate(AUTH_JWT) {
        route("/explore") {
            get("/popular") {
                val page = call.parameters["page"]?.toIntOrNull() ?: 0
                val size = (call.parameters["size"]?.toIntOrNull() ?: 10).coerceIn(1, 50)
                call.respond(ApiResponse.ok(service.popular(page, size).toResponse { it.toResponse() }))
            }
            get("/new") {
                val page = call.parameters["page"]?.toIntOrNull() ?: 0
                val size = (call.parameters["size"]?.toIntOrNull() ?: 10).coerceIn(1, 50)
                call.respond(ApiResponse.ok(service.newest(page, size).toResponse { it.toResponse() }))
            }
            get("/recommended") {
                val limit = (call.parameters["limit"]?.toIntOrNull() ?: 10).coerceIn(1, 50)
                call.respond(ApiResponse.ok(service.recommended(call.userId(), limit).map { it.toResponse() }))
            }
        }
    }
}
