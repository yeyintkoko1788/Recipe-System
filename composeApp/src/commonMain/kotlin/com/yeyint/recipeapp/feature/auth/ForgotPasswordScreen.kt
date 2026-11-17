package com.yeyint.recipeapp.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.displayMessage
import com.yeyint.recipeapp.ui.components.AppButton
import com.yeyint.recipeapp.ui.components.AppTextField
import com.yeyint.recipeapp.ui.components.PasswordTextField
import org.koin.compose.viewmodel.koinViewModel

/** Request a reset code by email, then set a new password with that code. */
@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordContract = koinViewModel<ForgotPasswordViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val error = (uiState as? ForgotPasswordUiState.Error)?.error
    val fieldErrors = (error as? AppError.Validation)?.fields.orEmpty()
    val step = (uiState as? ForgotPasswordUiState.Error)?.step ?: uiState
    val codeStep = step is ForgotPasswordUiState.EnterCode || step is ForgotPasswordUiState.Resetting

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (step is ForgotPasswordUiState.Done) {
            Text("Password updated", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "You can now log in with your new password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            AppButton(text = "Back to log in", onClick = onBackToLogin)
            return@Column
        }

        Text("Reset password", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            if (codeStep) "Enter the 6-digit code we emailed to $email and choose a new password."
            else "Enter your account email and we'll send you a reset code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        if (!codeStep) {
            AppTextField(
                value = email,
                onValueChange = { email = it; viewModel.consumeError() },
                label = "Email",
                keyboardType = KeyboardType.Email,
                errorMessage = fieldErrors["email"],
            )
        } else {
            AppTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6); viewModel.consumeError() },
                label = "6-digit code",
                keyboardType = KeyboardType.NumberPassword,
                errorMessage = fieldErrors["code"],
            )
            Spacer(Modifier.height(12.dp))
            PasswordTextField(
                value = newPassword,
                onValueChange = { newPassword = it; viewModel.consumeError() },
                label = "New password",
                errorMessage = fieldErrors["newPassword"],
            )
            Spacer(Modifier.height(12.dp))
            PasswordTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; viewModel.consumeError() },
                label = "Confirm new password",
                errorMessage = fieldErrors["confirmPassword"],
            )
        }

        if (error != null && fieldErrors.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                error.displayMessage(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(24.dp))
        if (!codeStep) {
            AppButton(
                text = "Send code",
                loading = step is ForgotPasswordUiState.SendingCode,
                onClick = { viewModel.requestCode(email) },
            )
        } else {
            AppButton(
                text = "Reset password",
                loading = step is ForgotPasswordUiState.Resetting,
                onClick = { viewModel.resetPassword(email, code, newPassword, confirmPassword) },
            )
            TextButton(onClick = { viewModel.requestCode(email) }) { Text("Resend code") }
        }
        TextButton(onClick = onBackToLogin) { Text("Back to log in") }
    }
}
