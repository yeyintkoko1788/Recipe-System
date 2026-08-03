package com.yeyint.recipeapp.backend.util

import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.security.JwtService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

/** The authenticated user's id, or 401 if the token is missing/invalid. */
fun ApplicationCall.userId(): UUID {
    val principal = principal<JWTPrincipal>() ?: throw AppException.Unauthorized()
    return runCatching { UUID.fromString(principal.subject) }
        .getOrElse { throw AppException.Unauthorized("Invalid token subject") }
}

/** The authenticated user's role claim. */
fun ApplicationCall.userRole(): UserRole {
    val principal = principal<JWTPrincipal>() ?: throw AppException.Unauthorized()
    val role = principal.payload.getClaim(JwtService.CLAIM_ROLE).asString()
        ?: throw AppException.Unauthorized("Missing role claim")
    return runCatching { UserRole.valueOf(role) }
        .getOrElse { throw AppException.Unauthorized("Unknown role") }
}

/** Throws 403 unless the caller is an admin. */
fun ApplicationCall.requireAdmin() {
    if (userRole() != UserRole.ADMIN) throw AppException.Forbidden("Admin access required")
}

/** Parses a UUID path parameter or fails with 400. */
fun ApplicationCall.uuidParameter(name: String): UUID {
    val raw = parameters[name] ?: throw AppException.Validation(fields = mapOf(name to "Missing parameter"))
    return runCatching { UUID.fromString(raw) }
        .getOrElse { throw AppException.Validation(fields = mapOf(name to "Must be a valid UUID")) }
}
