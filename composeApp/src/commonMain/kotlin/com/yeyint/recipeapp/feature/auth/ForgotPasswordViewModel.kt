package com.yeyint.recipeapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface ForgotPasswordContract {
    val uiState: StateFlow<ForgotPasswordUiState>
    fun requestCode(email: String)
    fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String)
    fun consumeError()
}

/** Two-step flow: ask for a code, then enter it with the new password. */
sealed interface ForgotPasswordUiState {
    data object EnterEmail : ForgotPasswordUiState
    data object SendingCode : ForgotPasswordUiState
    data object EnterCode : ForgotPasswordUiState
    data object Resetting : ForgotPasswordUiState
    data object Done : ForgotPasswordUiState
    /** [step] is the step to return to once the error is dismissed. */
    data class Error(val error: AppError, val step: ForgotPasswordUiState) : ForgotPasswordUiState
}

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel(), ForgotPasswordContract {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.EnterEmail)
    override val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    override fun requestCode(email: String) {
        if (_uiState.value is ForgotPasswordUiState.SendingCode) return
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error(
                AppError.Validation("Please fix the errors below", mapOf("email" to "Email is required")),
                ForgotPasswordUiState.EnterEmail,
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState.SendingCode
            authRepository.forgotPassword(email)
                .onSuccess { _uiState.value = ForgotPasswordUiState.EnterCode }
                .onFailure { _uiState.value = ForgotPasswordUiState.Error(it, ForgotPasswordUiState.EnterEmail) }
        }
    }

    override fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String) {
        if (_uiState.value is ForgotPasswordUiState.Resetting) return
        val fieldErrors = buildMap {
            if (code.trim().length != 6) put("code", "Enter the 6-digit code")
            if (newPassword.length < 8) put("newPassword", "Password must be at least 8 characters")
            if (newPassword != confirmPassword) put("confirmPassword", "Passwords do not match")
        }
        if (fieldErrors.isNotEmpty()) {
            _uiState.value = ForgotPasswordUiState.Error(
                AppError.Validation("Please fix the errors below", fieldErrors),
                ForgotPasswordUiState.EnterCode,
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState.Resetting
            authRepository.resetPassword(email, code, newPassword)
                .onSuccess { _uiState.value = ForgotPasswordUiState.Done }
                .onFailure { _uiState.value = ForgotPasswordUiState.Error(it, ForgotPasswordUiState.EnterCode) }
        }
    }

    override fun consumeError() {
        (_uiState.value as? ForgotPasswordUiState.Error)?.let { _uiState.value = it.step }
    }
}
