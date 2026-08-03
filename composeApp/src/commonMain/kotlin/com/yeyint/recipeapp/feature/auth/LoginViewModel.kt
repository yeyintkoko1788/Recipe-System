package com.yeyint.recipeapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.User
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Contract-first ViewModel (pattern carried over from ComposeBase's
 * LoginContract): the interface makes fakes trivial in UI tests.
 */
interface LoginContract {
    val uiState: StateFlow<LoginUiState>
    fun login(email: String, password: String)
    fun consumeError()
}

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: User) : LoginUiState
    data class Error(val error: AppError) : LoginUiState
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel(), LoginContract {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    override val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    override fun login(email: String, password: String) {
        if (_uiState.value is LoginUiState.Loading) return

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error(
                AppError.Validation("Please enter your email and password"),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.login(email, password)
                .onSuccess { _uiState.value = LoginUiState.Success(it) }
                .onFailure { error ->
                    // A 401 during login means bad credentials, not an expired session.
                    val mapped = if (error == AppError.SessionExpired) {
                        AppError.Validation("Invalid email or password")
                    } else error
                    _uiState.value = LoginUiState.Error(mapped)
                }
        }
    }

    override fun consumeError() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}
