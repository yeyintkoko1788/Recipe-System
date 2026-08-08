package com.yeyint.recipeapp.shared.data.remote

import com.yeyint.recipeapp.shared.data.remote.dto.ApiEnvelope
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.AppResult
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException

/**
 * Executes [block], unwraps the API envelope and maps every failure mode to a
 * typed [AppError]. This is the single choke point between HTTP and domain —
 * repositories never see exceptions or raw status codes.
 */
suspend inline fun <reified T> safeApiCall(block: () -> HttpResponse): AppResult<T> = try {
    val response = block()
    val envelope = response.body<ApiEnvelope<T>>()
    when {
        envelope.success && envelope.data != null -> AppResult.Success(envelope.data)
        response.status == HttpStatusCode.Unauthorized -> AppResult.Failure(AppError.SessionExpired)
        envelope.error != null -> {
            val error = envelope.error
            if (error!!.code == "VALIDATION_ERROR") {
                AppResult.Failure(AppError.Validation(error.message, error.details ?: emptyMap()))
            } else {
                AppResult.Failure(AppError.Api(error.code, error.message))
            }
        }
        else -> AppResult.Failure(AppError.Unknown())
    }
} catch (e: CancellationException) {
    throw e
} catch (e: HttpRequestTimeoutException) {
    AppResult.Failure(AppError.Timeout)
} catch (e: IOException) {
    AppResult.Failure(AppError.Network)
} catch (e: Exception) {
    AppResult.Failure(AppError.Unknown(e.message))
}
