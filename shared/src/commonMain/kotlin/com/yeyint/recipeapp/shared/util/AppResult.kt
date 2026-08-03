package com.yeyint.recipeapp.shared.util

/**
 * Result type returned by every repository operation.
 *
 * A deliberately simplified evolution of ComposeBase's `FlowReturnResult`:
 * the long list of ad-hoc cases collapses into [Success] / [Failure] with a
 * typed [AppError], which keeps `when` branches exhaustive and small.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>

    val isSuccess: Boolean get() = this is Success
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

/** Typed error cases the UI can render meaningfully. */
sealed interface AppError {
    /** No connectivity / DNS / socket failures. */
    data object Network : AppError

    /** Request timed out. */
    data object Timeout : AppError

    /** Access + refresh token are both invalid — user must log in again. */
    data object SessionExpired : AppError

    /** 400 with per-field messages from the backend. */
    data class Validation(val message: String, val fields: Map<String, String> = emptyMap()) : AppError

    /** Backend rejected the request (404, 409, 403 …). */
    data class Api(val code: String, val message: String) : AppError

    /** Anything unexpected. */
    data class Unknown(val message: String? = null) : AppError
}

/** Human-readable fallback message; screens may override per error type. */
fun AppError.displayMessage(): String = when (this) {
    AppError.Network -> "No internet connection. Please check your network."
    AppError.Timeout -> "The request timed out. Please try again."
    AppError.SessionExpired -> "Your session has expired. Please log in again."
    is AppError.Validation -> message
    is AppError.Api -> message
    is AppError.Unknown -> message ?: "Something went wrong. Please try again."
}
