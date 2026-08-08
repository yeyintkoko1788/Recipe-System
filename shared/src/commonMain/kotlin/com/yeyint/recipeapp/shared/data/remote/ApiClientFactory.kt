package com.yeyint.recipeapp.shared.data.remote

import com.yeyint.recipeapp.shared.data.local.TokenStorage
import com.yeyint.recipeapp.shared.data.remote.dto.ApiEnvelope
import com.yeyint.recipeapp.shared.data.remote.dto.AuthResponseDto
import com.yeyint.recipeapp.shared.data.remote.dto.RefreshTokenRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Base configuration injected at app start (per flavor / build config). */
data class ApiConfig(
    val baseUrl: String,
    val enableNetworkLogs: Boolean = false,
)

/**
 * Builds the shared [HttpClient]:
 *  - kotlinx.serialization JSON,
 *  - bearer auth with transparent refresh-token rotation,
 *  - timeouts and optional wire logging.
 *
 * When the refresh call itself fails the session is cleared and
 * [onSessionExpired] fires, which the auth layer translates into a global
 * logout (the KMP equivalent of ComposeBase's AuthEventBus + interceptors).
 */
fun createHttpClient(
    engine: HttpClientEngine,
    config: ApiConfig,
    tokenStorage: TokenStorage,
    onSessionExpired: () -> Unit,
): HttpClient = HttpClient(engine) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }

    if (config.enableNetworkLogs) {
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }
        install(InspektifyKtor)
    }

    install(Auth) {
        bearer {
            loadTokens {
                tokenStorage.accessToken?.let { access ->
                    BearerTokens(access, tokenStorage.refreshToken ?: "")
                }
            }
            refreshTokens {
                val refreshToken = tokenStorage.refreshToken
                    ?: return@refreshTokens null.also { onSessionExpired() }
                val response = runCatching {
                    client.post("${config.baseUrl}/api/v1/auth/refresh") {
                        markAsRefreshTokenRequest()
                        contentType(ContentType.Application.Json)
                        setBody(RefreshTokenRequestDto(refreshToken))
                    }.body<ApiEnvelope<AuthResponseDto>>()
                }.getOrNull()

                val auth = response?.takeIf { it.success }?.data
                if (auth == null) {
                    tokenStorage.clear()
                    onSessionExpired()
                    null
                } else {
                    tokenStorage.accessToken = auth.accessToken
                    tokenStorage.refreshToken = auth.refreshToken
                    BearerTokens(auth.accessToken, auth.refreshToken)
                }
            }
            sendWithoutRequest { request ->
                // Skip the auth header for the public auth endpoints.
                !request.url.encodedPath.contains("/auth/")
            }
        }
    }

    defaultRequest {
        url.takeFrom(config.baseUrl)
        contentType(ContentType.Application.Json)
    }
}
