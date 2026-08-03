package com.yeyint.recipeapp.backend.routes

import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.dto.MessageResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** Liveness probe for Docker/Kubernetes health checks. */
fun Route.healthRoutes() {
    get("/healthz") {
        call.respond(ApiResponse.ok(MessageResponse("OK")))
    }
}
