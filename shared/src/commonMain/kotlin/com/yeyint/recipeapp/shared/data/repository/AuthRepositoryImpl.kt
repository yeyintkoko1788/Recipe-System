package com.yeyint.recipeapp.shared.data.repository

import com.yeyint.recipeapp.shared.data.local.TokenStorage
import com.yeyint.recipeapp.shared.data.remote.api.AuthApi
import com.yeyint.recipeapp.shared.data.remote.dto.AuthResponseDto
import com.yeyint.recipeapp.shared.data.remote.dto.LoginRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.RegisterRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.toDomain
import com.yeyint.recipeapp.shared.domain.model.User
import com.yeyint.recipeapp.shared.domain.model.UserRole
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.domain.repository.AuthState
import com.yeyint.recipeapp.shared.util.AppResult
import com.yeyint.recipeapp.shared.util.map
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Restores the persisted session on app start (offline-friendly). */
    override suspend fun restoreSession() {
        if (!tokenStorage.hasSession) {
            _authState.value = AuthState.LoggedOut
            return
        }
        val cached = cachedUser()
        _authState.value = if (cached != null) AuthState.LoggedIn(cached) else AuthState.LoggedOut
    }

    override suspend fun login(email: String, password: String): AppResult<User> =
        api.login(LoginRequestDto(email.trim(), password))
            .onSuccess(::persistSession)
            .map { it.user.toDomain() }

    override suspend fun register(name: String, email: String, password: String): AppResult<User> =
        api.register(RegisterRequestDto(name.trim(), email.trim(), password))
            .onSuccess(::persistSession)
            .map { it.user.toDomain() }

    override suspend fun logout(): AppResult<Unit> {
        // Best effort server-side revocation; local logout always succeeds.
        tokenStorage.refreshToken?.let { api.logout(it) }
        tokenStorage.clear()
        _authState.value = AuthState.LoggedOut
        return AppResult.Success(Unit)
    }

    override fun onSessionExpired() {
        tokenStorage.clear()
        _authState.value = AuthState.LoggedOut
    }

    private fun persistSession(auth: AuthResponseDto) {
        tokenStorage.accessToken = auth.accessToken
        tokenStorage.refreshToken = auth.refreshToken
        tokenStorage.userId = auth.user.id
        tokenStorage.userName = auth.user.name
        tokenStorage.userEmail = auth.user.email
        tokenStorage.userRole = auth.user.role
        _authState.value = AuthState.LoggedIn(auth.user.toDomain())
    }

    private fun cachedUser(): User? {
        val id = tokenStorage.userId ?: return null
        return User(
            id = id,
            name = tokenStorage.userName ?: return null,
            email = tokenStorage.userEmail ?: return null,
            role = UserRole.entries.firstOrNull { it.name == tokenStorage.userRole } ?: UserRole.USER,
        )
    }
}
