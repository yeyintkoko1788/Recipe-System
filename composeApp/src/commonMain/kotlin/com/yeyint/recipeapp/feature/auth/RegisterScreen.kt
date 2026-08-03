package com.yeyint.recipeapp.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    viewModel: RegisterContract = koinViewModel<RegisterViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val error = (uiState as? RegisterUiState.Error)?.error
    val fieldErrors = (error as? AppError.Validation)?.fields.orEmpty()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(28.dp))

        AppTextField(
            value = name,
            onValueChange = { name = it; viewModel.consumeError() },
            label = "Name",
            errorMessage = fieldErrors["name"],
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = email,
            onValueChange = { email = it; viewModel.consumeError() },
            label = "Email",
            keyboardType = KeyboardType.Email,
            errorMessage = fieldErrors["email"],
        )
        Spacer(Modifier.height(12.dp))
        PasswordTextField(
            value = password,
            onValueChange = { password = it; viewModel.consumeError() },
            errorMessage = fieldErrors["password"],
        )
        Spacer(Modifier.height(12.dp))
        PasswordTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; viewModel.consumeError() },
            label = "Confirm password",
            errorMessage = fieldErrors["confirmPassword"],
        )

        if (error != null && fieldErrors.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                error.displayMessage(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(24.dp))
        AppButton(
            text = "Sign up",
            loading = uiState is RegisterUiState.Loading,
            onClick = { viewModel.register(name, email, password, confirmPassword) },
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBackToLogin) { Text("Already have an account? Log in") }
    }
}
