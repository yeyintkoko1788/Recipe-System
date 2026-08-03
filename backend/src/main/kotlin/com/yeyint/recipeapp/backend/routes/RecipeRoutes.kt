package com.yeyint.recipeapp.backend.routes

import com.yeyint.recipeapp.backend.domain.Difficulty
import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.dto.MessageResponse
import com.yeyint.recipeapp.backend.dto.RecipeRequest
import com.yeyint.recipeapp.backend.dto.toResponse
import com.yeyint.recipeapp.backend.plugins.AUTH_JWT
import com.yeyint.recipeapp.backend.repository.RecipeFilter
import com.yeyint.recipeapp.backend.service.RecipeService
import com.yeyint.recipeapp.backend.util.pageRequest
import com.yeyint.recipeapp.backend.util.userId
import com.yeyint.recipeapp.backend.util.userRole
import com.yeyint.recipeapp.backend.util.uuidParameter
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.recipeRoutes(service: RecipeService) {
    authenticate(AUTH_JWT) {
        route("/recipes") {
            // GET /recipes?query=&difficulty=&maxTime=&mine=&page=&size=&sort=&order=
            get {
                val page = call.pageRequest(
                    allowedSorts = setOf("createdAt", "title", "cookingTime", "popularity"),
                    defaultSort = "createdAt",
                )
                val filter = RecipeFilter(
                    query = call.parameters["query"],
                    difficulty = call.parameters["difficulty"]?.let {
                        runCatching { Difficulty.valueOf(it.uppercase()) }.getOrNull()
                    },
                    maxCookingTimeMinutes = call.parameters["maxTime"]?.toIntOrNull(),
                    createdBy = if (call.parameters["mine"] == "true") call.userId() else null,
                )
                call.respond(ApiResponse.ok(service.search(filter, page).toResponse { it.toResponse() }))
            }
            get("/{id}") {
                call.respond(ApiResponse.ok(service.get(call.uuidParameter("id")).toResponse()))
            }
            post {
                val created = service.create(call.userId(), call.receive<RecipeRequest>())
                call.respond(HttpStatusCode.Created, ApiResponse.ok(created.toResponse()))
            }
            put("/{id}") {
                val updated = service.update(
                    call.uuidParameter("id"), call.userId(), call.userRole(), call.receive<RecipeRequest>(),
                )
                call.respond(ApiResponse.ok(updated.toResponse()))
            }
            delete("/{id}") {
                service.delete(call.uuidParameter("id"), call.userId(), call.userRole())
                call.respond(ApiResponse.ok(MessageResponse("Recipe deleted")))
            }
        }
    }
}
