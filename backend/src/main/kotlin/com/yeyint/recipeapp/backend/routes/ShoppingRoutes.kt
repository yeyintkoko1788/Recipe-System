package com.yeyint.recipeapp.backend.routes

import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.dto.MessageResponse
import com.yeyint.recipeapp.backend.dto.PurchaseRequest
import com.yeyint.recipeapp.backend.dto.ShoppingAddRequest
import com.yeyint.recipeapp.backend.dto.toResponse
import com.yeyint.recipeapp.backend.plugins.AUTH_JWT
import com.yeyint.recipeapp.backend.service.ShoppingService
import com.yeyint.recipeapp.backend.util.userId
import com.yeyint.recipeapp.backend.util.uuidParameter
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.shoppingRoutes(service: ShoppingService) {
    authenticate(AUTH_JWT) {
        route("/shopping-list") {
            get {
                val includePurchased = call.parameters["includePurchased"] == "true"
                call.respond(ApiResponse.ok(service.list(call.userId(), includePurchased).map { it.toResponse() }))
            }
            post {
                val item = service.add(call.userId(), call.receive<ShoppingAddRequest>())
                call.respond(HttpStatusCode.Created, ApiResponse.ok(item.toResponse()))
            }
            /** Auto-fill from out-of-stock pantry items. */
            post("/generate") {
                call.respond(ApiResponse.ok(service.generateFromPantry(call.userId()).map { it.toResponse() }))
            }
            post("/{id}/purchase") {
                val request = runCatching { call.receive<PurchaseRequest>() }.getOrElse { PurchaseRequest() }
                call.respond(ApiResponse.ok(service.purchase(call.userId(), call.uuidParameter("id"), request).toResponse()))
            }
            delete("/purchased") {
                val removed = service.clearPurchased(call.userId())
                call.respond(ApiResponse.ok(MessageResponse("Removed $removed purchased item(s)")))
            }
            delete("/{id}") {
                service.delete(call.userId(), call.uuidParameter("id"))
                call.respond(ApiResponse.ok(MessageResponse("Item removed")))
            }
        }
    }
}
