package com.yeyint.recipeapp.backend.plugins

import com.yeyint.recipeapp.backend.dto.ApiError
import com.yeyint.recipeapp.backend.dto.ApiResponse
import com.yeyint.recipeapp.backend.util.AppException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

/**
 * Centralized exception → HTTP mapping. Every error leaves the API in the same
 * envelope shape: `{ "success": false, "error": { code, message, details } }`.
 */
fun Application.configureStatusPages() {
    val log = LoggerFactory.getLogger("ErrorHandler")

    install(StatusPages) {
        exception<AppException> { call, cause ->
            val (status, code) = when (cause) {
                is AppException.Validation -> HttpStatusCode.BadRequest to "VALIDATION_ERROR"
                is AppException.Unauthorized -> HttpStatusCode.Unauthorized to "UNAUTHORIZED"
                is AppException.Forbidden -> HttpStatusCode.Forbidden to "FORBIDDEN"
                is AppException.NotFound -> HttpStatusCode.NotFound to "NOT_FOUND"
                is AppException.Conflict -> HttpStatusCode.Conflict to "CONFLICT"
            }
            val details = (cause as? AppException.Validation)?.fields?.takeIf { it.isNotEmpty() }
            call.respond(status, ApiResponse.failure(code, cause.message ?: "Error", details))
        }
        exception<JsonConvertException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure("MALFORMED_BODY", cause.message ?: "Malformed request body"),
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure("BAD_REQUEST", cause.message ?: "Bad request"),
            )
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse.failure("INTERNAL_ERROR", "Something went wrong"),
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ApiResponse.failure("NOT_FOUND", "Route not found"))
        }
    }
}
