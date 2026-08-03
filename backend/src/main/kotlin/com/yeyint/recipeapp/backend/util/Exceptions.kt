package com.yeyint.recipeapp.backend.util

/**
 * Domain-level exceptions thrown by services. The StatusPages plugin maps each
 * one to a stable HTTP status + error code, keeping HTTP concerns out of the
 * business layer.
 */
sealed class AppException(message: String) : RuntimeException(message) {

    /** 400 — one or more request fields failed validation. */
    class Validation(
        message: String = "Validation failed",
        val fields: Map<String, String> = emptyMap(),
    ) : AppException(message)

    /** 401 — missing/invalid credentials or token. */
    class Unauthorized(message: String = "Unauthorized") : AppException(message)

    /** 403 — authenticated but not allowed to perform the action. */
    class Forbidden(message: String = "Forbidden") : AppException(message)

    /** 404 — entity does not exist (or is soft-deleted). */
    class NotFound(message: String = "Resource not found") : AppException(message)

    /** 409 — uniqueness/state conflict, e.g. duplicate ingredient name. */
    class Conflict(message: String) : AppException(message)
}

/** Small helper used by services to collect field errors before failing. */
class ValidationErrors {
    private val errors = mutableMapOf<String, String>()

    fun require(condition: Boolean, field: String, message: String) {
        if (!condition) errors.putIfAbsent(field, message)
    }

    fun reject(field: String, message: String) {
        errors.putIfAbsent(field, message)
    }

    fun throwIfAny() {
        if (errors.isNotEmpty()) throw AppException.Validation(fields = errors.toMap())
    }
}

inline fun validate(block: ValidationErrors.() -> Unit) {
    ValidationErrors().apply(block).throwIfAny()
}
