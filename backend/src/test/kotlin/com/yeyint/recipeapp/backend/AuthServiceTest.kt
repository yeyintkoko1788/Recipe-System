package com.yeyint.recipeapp.backend

import com.yeyint.recipeapp.backend.config.JwtConfig
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.dto.LoginRequest
import com.yeyint.recipeapp.backend.dto.RegisterRequest
import com.yeyint.recipeapp.backend.security.JwtService
import com.yeyint.recipeapp.backend.service.AuthService
import com.yeyint.recipeapp.backend.util.AppException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthServiceTest {

    private val users = FakeUserRepository()
    private val tokens = FakeRefreshTokenRepository()
    private val jwt = JwtService(
        JwtConfig(
            secret = "test-secret", issuer = "test", audience = "test",
            accessTtlMinutes = 15, refreshTtlDays = 30,
        ),
    )
    private val service = AuthService(users, tokens, FakePasswordHasher(), jwt)

    @Test
    fun `register creates a USER account and returns tokens`() = runTest {
        val response = service.register(RegisterRequest("Ye Yint", "yeyint@example.com", "password1"))

        assertEquals("yeyint@example.com", response.user.email)
        assertEquals(UserRole.USER.name, response.user.role)
        assertTrue(response.accessToken.isNotBlank())
        assertTrue(response.refreshToken.isNotBlank())
    }

    @Test
    fun `register rejects weak passwords with field errors`() = runTest {
        val exception = assertFailsWith<AppException.Validation> {
            service.register(RegisterRequest("Ye Yint", "yeyint@example.com", "short"))
        }
        assertTrue("password" in exception.fields)
    }

    @Test
    fun `register rejects duplicate emails case-insensitively`() = runTest {
        service.register(RegisterRequest("A", "user@example.com", "password1"))
        assertFailsWith<AppException.Conflict> {
            service.register(RegisterRequest("B", "USER@Example.COM", "password2"))
        }
    }

    @Test
    fun `login fails with the same message for wrong email and wrong password`() = runTest {
        service.register(RegisterRequest("A", "user@example.com", "password1"))

        val wrongPassword = assertFailsWith<AppException.Unauthorized> {
            service.login(LoginRequest("user@example.com", "nope12345"))
        }
        val wrongEmail = assertFailsWith<AppException.Unauthorized> {
            service.login(LoginRequest("ghost@example.com", "password1"))
        }
        assertEquals(wrongPassword.message, wrongEmail.message)
    }

    @Test
    fun `refresh rotates the token - old token stops working`() = runTest {
        val registered = service.register(RegisterRequest("A", "user@example.com", "password1"))

        val refreshed = service.refresh(registered.refreshToken)
        assertNotEquals(registered.refreshToken, refreshed.refreshToken)

        // The original token was revoked by the rotation.
        assertFailsWith<AppException.Unauthorized> { service.refresh(registered.refreshToken) }
    }

    @Test
    fun `logout revokes the refresh token`() = runTest {
        val registered = service.register(RegisterRequest("A", "user@example.com", "password1"))
        service.logout(registered.refreshToken)
        assertFailsWith<AppException.Unauthorized> { service.refresh(registered.refreshToken) }
    }
}
