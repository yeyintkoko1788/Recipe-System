package com.yeyint.recipeapp.backend.routes

import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.dto.MessageResponse
import com.yeyint.recipeapp.backend.dto.ModerateIngredientRequest
import com.yeyint.recipeapp.backend.dto.UpdateUserRoleRequest
import com.yeyint.recipeapp.backend.dto.toResponse
import com.yeyint.recipeapp.backend.plugins.AUTH_JWT
import com.yeyint.recipeapp.backend.service.AdminService
import com.yeyint.recipeapp.backend.service.IngredientService
import com.yeyint.recipeapp.backend.util.pageRequest
import com.yeyint.recipeapp.backend.util.requireAdmin
import com.yeyint.recipeapp.backend.util.userId
import com.yeyint.recipeapp.backend.util.uuidParameter
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route

/**
 * Admin API. Every handler starts with [requireAdmin]; recipe/ingredient
 * moderation reuses the standard endpoints (admins pass the ownership checks).
 */
fun Route.adminRoutes(service: AdminService, ingredientService: IngredientService) {
    authenticate(AUTH_JWT) {
        route("/admin") {
            get("/users") {
                call.requireAdmin()
                val page = call.pageRequest(allowedSorts = setOf("createdAt", "name", "email"), defaultSort = "createdAt")
                val result = service.listUsers(page, call.parameters["search"])
                call.respond(ApiResponse.ok(result.toResponse { it.toResponse() }))
            }
            patch("/users/{id}/role") {
                call.requireAdmin()
                val request = call.receive<UpdateUserRoleRequest>()
                val updated = service.updateUserRole(call.userId(), call.uuidParameter("id"), request.role)
                call.respond(ApiResponse.ok(updated.toResponse()))
            }
            delete("/users/{id}") {
                call.requireAdmin()
                service.deleteUser(call.userId(), call.uuidParameter("id"))
                call.respond(ApiResponse.ok(MessageResponse("User deleted")))
            }
            patch("/ingredients/{id}/status") {
                call.requireAdmin()
                val request = call.receive<ModerateIngredientRequest>()
                val updated = ingredientService.moderate(call.uuidParameter("id"), request.status)
                call.respond(ApiResponse.ok(updated.toResponse()))
            }
            get("/stats") {
                call.requireAdmin()
                call.respond(ApiResponse.ok(service.statistics()))
            }
        }
    }
}
