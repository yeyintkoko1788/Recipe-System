package com.yeyint.recipeapp.shared.data.remote.api

import com.yeyint.recipeapp.shared.data.remote.dto.AuthResponseDto
import com.yeyint.recipeapp.shared.data.remote.dto.LoginRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.MessageDto
import com.yeyint.recipeapp.shared.data.remote.dto.RefreshTokenRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.RegisterRequestDto
import com.yeyint.recipeapp.shared.data.remote.dto.UserDto
import com.yeyint.recipeapp.shared.data.remote.safeApiCall
import com.yeyint.recipeapp.shared.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthApi(private val client: HttpClient) {

    suspend fun register(request: RegisterRequestDto): AppResult<AuthResponseDto> =
        safeApiCall { client.post("api/v1/auth/register") { setBody(request) } }

    suspend fun login(request: LoginRequestDto): AppResult<AuthResponseDto> =
        safeApiCall { client.post("api/v1/auth/login") { setBody(request) } }

    suspend fun logout(refreshToken: String): AppResult<MessageDto> =
        safeApiCall { client.post("api/v1/auth/logout") { setBody(RefreshTokenRequestDto(refreshToken)) } }

    suspend fun me(): AppResult<UserDto> =
        safeApiCall { client.get("api/v1/auth/me") }
}
