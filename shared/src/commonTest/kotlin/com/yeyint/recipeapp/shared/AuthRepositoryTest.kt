package com.yeyint.recipeapp.shared

import com.russhwolf.settings.MapSettings
import com.yeyint.recipeapp.shared.data.local.TokenStorage
import com.yeyint.recipeapp.shared.data.remote.api.AuthApi
import com.yeyint.recipeapp.shared.data.repository.AuthRepositoryImpl
import com.yeyint.recipeapp.shared.domain.repository.AuthState
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthRepositoryTest {

    private fun client(status: HttpStatusCode, body: String) = HttpClient(
        MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
    }

    private val successBody = """
        {"success":true,"data":{
            "user":{"id":"u1","name":"Ye Yint","email":"ye@example.com","role":"USER","createdAt":"now"},
            "accessToken":"access-1","refreshToken":"refresh-1","expiresInSeconds":900
        }}
    """.trimIndent()

    @Test
    fun `successful login persists tokens and flips auth state`() = runTest {
        val storage = TokenStorage(MapSettings())
        val repository = AuthRepositoryImpl(AuthApi(client(HttpStatusCode.OK, successBody)), storage)

        val result = repository.login("ye@example.com", "password1")

        assertIs<AppResult.Success<*>>(result)
        assertEquals("access-1", storage.accessToken)
        assertEquals("refresh-1", storage.refreshToken)
        assertIs<AuthState.LoggedIn>(repository.authState.value)
    }

    @Test
    fun `failed login maps the envelope error and stores nothing`() = runTest {
        val errorBody = """{"success":false,"error":{"code":"UNAUTHORIZED","message":"Invalid email or password"}}"""
        val storage = TokenStorage(MapSettings())
        val repository = AuthRepositoryImpl(AuthApi(client(HttpStatusCode.Unauthorized, errorBody)), storage)

        val result = repository.login("ye@example.com", "wrong")

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.SessionExpired, result.error) // 401 during login == invalid credentials path
        assertEquals(null, storage.accessToken)
    }

    @Test
    fun `restoreSession without tokens is logged out`() = runTest {
        val repository = AuthRepositoryImpl(
            AuthApi(client(HttpStatusCode.OK, successBody)),
            TokenStorage(MapSettings()),
        )
        repository.restoreSession()
        assertEquals(AuthState.LoggedOut, repository.authState.value)
    }

    @Test
    fun `restoreSession with cached user is logged in without a network call`() = runTest {
        val settings = MapSettings()
        val storage = TokenStorage(settings).apply {
            accessToken = "a"; refreshToken = "r"
            userId = "u1"; userName = "Ye Yint"; userEmail = "ye@example.com"; userRole = "USER"
        }
        val repository = AuthRepositoryImpl(AuthApi(client(HttpStatusCode.OK, successBody)), storage)

        repository.restoreSession()

        val state = repository.authState.value
        assertIs<AuthState.LoggedIn>(state)
        assertEquals("Ye Yint", state.user.name)
    }

    @Test
    fun `logout clears the stored session`() = runTest {
        val storage = TokenStorage(MapSettings())
        val repository = AuthRepositoryImpl(AuthApi(client(HttpStatusCode.OK, successBody)), storage)
        repository.login("ye@example.com", "password1")

        repository.logout()

        assertTrue(!storage.hasSession)
        assertEquals(AuthState.LoggedOut, repository.authState.value)
    }
}
