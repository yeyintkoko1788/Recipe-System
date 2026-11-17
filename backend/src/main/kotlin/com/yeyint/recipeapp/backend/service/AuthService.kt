package com.yeyint.recipeapp.backend.service

import com.yeyint.recipeapp.backend.config.AdminConfig
import com.yeyint.recipeapp.backend.domain.User
import com.yeyint.recipeapp.backend.domain.UserRole
import com.yeyint.recipeapp.backend.dto.AuthResponse
import com.yeyint.recipeapp.backend.dto.LoginRequest
import com.yeyint.recipeapp.backend.dto.RegisterRequest
import com.yeyint.recipeapp.backend.dto.toResponse
import com.yeyint.recipeapp.backend.dto.ResetPasswordRequest
import com.yeyint.recipeapp.backend.mail.Mailer
import com.yeyint.recipeapp.backend.repository.PasswordResetRepository
import com.yeyint.recipeapp.backend.repository.RefreshTokenRepository
import com.yeyint.recipeapp.backend.repository.UserRepository
import com.yeyint.recipeapp.backend.security.JwtService
import com.yeyint.recipeapp.backend.security.PasswordHasher
import com.yeyint.recipeapp.backend.util.AppException
import com.yeyint.recipeapp.backend.util.validate
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Registration, login and token lifecycle.
 *
 * Refresh tokens are rotated on every use: the presented token is revoked and
 * a new one issued, so a stolen token stops working the moment the legitimate
 * client refreshes.
 */
class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val hasher: PasswordHasher,
    private val jwt: JwtService,
    private val passwordResets: PasswordResetRepository,
    private val mailer: Mailer,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val random = SecureRandom()

    suspend fun register(request: RegisterRequest): AuthResponse {
        val name = request.name.trim()
        val email = request.email.trim().lowercase()
        validate {
            require(name.length in 2..100, "name", "Name must be 2–100 characters")
            require(EMAIL_REGEX.matches(email), "email", "Must be a valid email address")
            require(request.password.length >= 8, "password", "Password must be at least 8 characters")
            require(
                request.password.any { it.isDigit() } && request.password.any { it.isLetter() },
                "password", "Password must contain letters and digits",
            )
        }
        if (users.findByEmail(email) != null) {
            throw AppException.Conflict("An account with this email already exists")
        }
        val user = users.create(name, email, hasher.hash(request.password), UserRole.USER)
        return issueTokens(user)
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val user = users.findByEmail(request.email.trim())
        // Constant error message: never reveal whether the email exists.
        if (user == null || !hasher.verify(request.password, user.passwordHash)) {
            throw AppException.Unauthorized("Invalid email or password")
        }
        return issueTokens(user)
    }

    suspend fun refresh(rawToken: String): AuthResponse {
        val hash = jwt.hash(rawToken)
        val userId = refreshTokens.findUserIdByValidToken(hash, Instant.now())
            ?: throw AppException.Unauthorized("Invalid or expired refresh token")
        val user = users.findById(userId) ?: throw AppException.Unauthorized("Account no longer exists")
        refreshTokens.revoke(hash) // rotation
        return issueTokens(user)
    }

    /**
     * Emails a 6-digit one-time code. Always succeeds from the caller's view so
     * the endpoint never reveals whether an email is registered.
     */
    suspend fun forgotPassword(email: String) {
        val user = users.findByEmail(email.trim().lowercase()) ?: return
        val code = "%06d".format(random.nextInt(1_000_000))
        passwordResets.store(user.id, jwt.hash(code + user.id), Instant.now().plus(RESET_CODE_TTL))
        mailer.sendPasswordResetCode(user.email, user.name, code, RESET_CODE_TTL.toMinutes())
    }

    suspend fun resetPassword(request: ResetPasswordRequest) {
        validate {
            require(
                request.newPassword.length >= 8 &&
                    request.newPassword.any { it.isDigit() } && request.newPassword.any { it.isLetter() },
                "newPassword", "Password must be at least 8 characters with letters and digits",
            )
        }
        val invalid = AppException.Validation("Invalid or expired code", mapOf("code" to "Invalid or expired code"))
        val user = users.findByEmail(request.email.trim().lowercase()) ?: throw invalid
        val owner = passwordResets.consume(jwt.hash(request.code.trim() + user.id), Instant.now())
        if (owner != user.id) throw invalid
        users.updatePassword(user.id, hasher.hash(request.newPassword))
        refreshTokens.revokeAllForUser(user.id) // sign out every existing session
    }

    suspend fun logout(rawToken: String) {
        refreshTokens.revoke(jwt.hash(rawToken))
    }

    suspend fun me(userId: UUID): User =
        users.findById(userId) ?: throw AppException.NotFound("User not found")

    /** Creates the bootstrap admin account on first startup (idempotent). */
    suspend fun ensureAdminUser(admin: AdminConfig) {
        if (users.findByEmail(admin.email) == null) {
            users.create(admin.name, admin.email.lowercase(), hasher.hash(admin.password), UserRole.ADMIN)
            log.info("Bootstrap admin account created: {}", admin.email)
        }
    }

    private suspend fun issueTokens(user: User): AuthResponse {
        val refreshToken = jwt.newRefreshToken()
        refreshTokens.store(user.id, jwt.hash(refreshToken), Instant.now().plus(jwt.refreshTokenTtl))
        return AuthResponse(
            user = user.toResponse(),
            accessToken = jwt.issueAccessToken(user.id, user.role),
            refreshToken = refreshToken,
            expiresInSeconds = jwt.accessTokenTtl.seconds,
        )
    }

    companion object {
        private val RESET_CODE_TTL: Duration = Duration.ofMinutes(15)
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
