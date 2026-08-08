package com.yeyint.recipeapp.shared

import com.yeyint.recipeapp.shared.data.remote.dto.MessageDto
import com.yeyint.recipeapp.shared.data.remote.safeApiCall
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SafeCallTest {

    private fun jsonClient(status: HttpStatusCode, body: String) = HttpClient(
        MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test
    fun `success envelope unwraps data`() = runTest {
        val client = jsonClient(HttpStatusCode.OK, """{"success":true,"data":{"message":"hi"}}""")
        val result = safeApiCall<MessageDto> { client.get("/") }
        assertEquals(AppResult.Success(MessageDto("hi")), result)
    }

    @Test
    fun `validation errors carry field details`() = runTest {
        val body = """
            {"success":false,"error":{"code":"VALIDATION_ERROR","message":"Validation failed",
             "details":{"title":"Title must be 3-200 characters"}}}
        """.trimIndent()
        val client = jsonClient(HttpStatusCode.BadRequest, body)

        val result = safeApiCall<MessageDto> { client.get("/") }

        assertIs<AppResult.Failure>(result)
        val error = result.error
        assertIs<AppError.Validation>(error)
        assertEquals("Title must be 3-200 characters", error.fields["title"])
    }

    @Test
    fun `conflict maps to a typed api error`() = runTest {
        val body = """{"success":false,"error":{"code":"CONFLICT","message":"Ingredient 'Salt' already exists"}}"""
        val client = jsonClient(HttpStatusCode.Conflict, body)

        val result = safeApiCall<MessageDto> { client.get("/") }

        assertIs<AppResult.Failure>(result)
        val error = result.error
        assertIs<AppError.Api>(error)
        assertEquals("CONFLICT", error.code)
    }

    @Test
    fun `io failures map to Network`() = runTest {
        val client = HttpClient(MockEngine { throw IOException("boom") }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val result = safeApiCall<MessageDto> { client.get("/") }
        assertEquals(AppResult.Failure(AppError.Network), result)
    }
}
