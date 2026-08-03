package com.yeyint.recipeapp.backend.routes

import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.dto.MessageResponse
import com.yeyint.recipeapp.backend.dto.PantryUpdateRequest
import com.yeyint.recipeapp.backend.dto.PantryUpsertRequest
import com.yeyint.recipeapp.backend.dto.RestockRequest
import com.yeyint.recipeapp.backend.dto.toResponse
import com.yeyint.recipeapp.backend.plugins.AUTH_JWT
import com.yeyint.recipeapp.backend.service.PantryService
import com.yeyint.recipeapp.backend.util.userId
import com.yeyint.recipeapp.backend.util.uuidParameter
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.pantryRoutes(service: PantryService) {
    authenticate(AUTH_JWT) {
        route("/pantry") {
            get {
                call.respond(ApiResponse.ok(service.list(call.userId()).map { it.toResponse() }))
            }
            post {
                val item = service.upsert(call.userId(), call.receive<PantryUpsertRequest>())
                call.respond(HttpStatusCode.Created, ApiResponse.ok(item.toResponse()))
            }
            patch("/{id}") {
                val item = service.update(call.userId(), call.uuidParameter("id"), call.receive<PantryUpdateRequest>())
                call.respond(ApiResponse.ok(item.toResponse()))
            }
            post("/{id}/out-of-stock") {
                call.respond(ApiResponse.ok(service.markOutOfStock(call.userId(), call.uuidParameter("id")).toResponse()))
            }
            post("/{id}/restock") {
                val request = call.receive<RestockRequest>()
                call.respond(
                    ApiResponse.ok(service.restock(call.userId(), call.uuidParameter("id"), request.quantity).toResponse()),
                )
            }
            delete("/{id}") {
                service.delete(call.userId(), call.uuidParameter("id"))
                call.respond(ApiResponse.ok(MessageResponse("Pantry item removed")))
            }
        }
    }
}
