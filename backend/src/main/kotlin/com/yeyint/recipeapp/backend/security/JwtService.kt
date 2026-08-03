package com.yeyint.recipeapp.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.yeyint.recipeapp.backend.config.JwtConfig
import com.yeyint.recipeapp.backend.domain.UserRole
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Issues and verifies tokens.
 *
 * Access tokens are short-lived signed JWTs carrying the user id and role.
 * Refresh tokens are opaque 256-bit random values; only their SHA-256 hash is
 * persisted, so a database leak cannot be replayed against the API.
 */
class JwtService(private val config: JwtConfig) {

    private val algorithm: Algorithm = Algorithm.HMAC256(config.secret)
    private val random = SecureRandom()

    val realm: String = "recipeapp"
    val accessTokenTtl: Duration = Duration.ofMinutes(config.accessTtlMinutes)
    val refreshTokenTtl: Duration = Duration.ofDays(config.refreshTtlDays)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun issueAccessToken(userId: UUID, role: UserRole): String {
        val now = Instant.now()
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withClaim(CLAIM_ROLE, role.name)
            .withIssuedAt(now)
            .withExpiresAt(now.plus(accessTokenTtl))
            .sign(algorithm)
    }

    /** Returns the raw refresh token; callers must store only [hash] of it. */
    fun newRefreshToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        const val CLAIM_ROLE = "role"
    }
}
