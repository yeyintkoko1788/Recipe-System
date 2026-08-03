package com.yeyint.recipeapp.backend.routes

import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.dto.IngredientRequest
import com.yeyint.recipeapp.backend.dto.MessageResponse
import com.yeyint.recipeapp.backend.dto.toResponse
import com.yeyint.recipeapp.backend.plugins.AUTH_JWT
import com.yeyint.recipeapp.backend.service.IngredientService
import com.yeyint.recipeapp.backend.util.pageRequest
import com.yeyint.recipeapp.backend.util.requireAdmin
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

fun Route.ingredientRoutes(service: IngredientService) {
    authenticate(AUTH_JWT) {
        route("/ingredients") {
            // GET /ingredients?search=&category=&status=&page=&size=&sort=name&order=asc
            get {
                val page = call.pageRequest(allowedSorts = setOf("name", "category", "createdAt"), defaultSort = "name")
                val result = service.list(
                    page = page,
                    search = call.parameters["search"],
                    category = call.parameters["category"],
                    status = call.parameters["status"],
                    callerRole = call.userRole(),
                )
                call.respond(ApiResponse.ok(result.toResponse { it.toResponse() }))
            }
            get("/{id}") {
                call.respond(ApiResponse.ok(service.get(call.uuidParameter("id")).toResponse()))
            }
            // Any user may submit; admin submissions are approved instantly.
            post {
                val created = service.create(call.receive<IngredientRequest>(), call.userId(), call.userRole())
                call.respond(HttpStatusCode.Created, ApiResponse.ok(created.toResponse()))
            }
            put("/{id}") {
                call.requireAdmin()
                val updated = service.update(call.uuidParameter("id"), call.receive<IngredientRequest>())
                call.respond(ApiResponse.ok(updated.toResponse()))
            }
            delete("/{id}") {
                call.requireAdmin()
                service.delete(call.uuidParameter("id"))
                call.respond(ApiResponse.ok(MessageResponse("Ingredient deleted")))
            }
        }
    }
}
