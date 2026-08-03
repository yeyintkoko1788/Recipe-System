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

interface RegisterContract {
    val uiState: StateFlow<RegisterUiState>
    fun register(name: String, email: String, password: String, confirmPassword: String)
    fun consumeError()
}

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Success(val user: User) : RegisterUiState
    data class Error(val error: AppError) : RegisterUiState
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel(), RegisterContract {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    override val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    override fun register(name: String, email: String, password: String, confirmPassword: String) {
        if (_uiState.value is RegisterUiState.Loading) return

        val fieldErrors = buildMap {
            if (name.trim().length < 2) put("name", "Name must be at least 2 characters")
            if (email.isBlank()) put("email", "Email is required")
            if (password.length < 8) put("password", "Password must be at least 8 characters")
            if (password != confirmPassword) put("confirmPassword", "Passwords do not match")
        }
        if (fieldErrors.isNotEmpty()) {
            _uiState.value = RegisterUiState.Error(AppError.Validation("Please fix the errors below", fieldErrors))
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            authRepository.register(name, email, password)
                .onSuccess { _uiState.value = RegisterUiState.Success(it) }
                .onFailure { _uiState.value = RegisterUiState.Error(it) }
        }
    }

    override fun consumeError() {
        if (_uiState.value is RegisterUiState.Error) _uiState.value = RegisterUiState.Idle
    }
}
