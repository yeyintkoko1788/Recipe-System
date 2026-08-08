package com.yeyint.recipeapp.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.displayMessage
import com.yeyint.recipeapp.ui.components.AppButton
import com.yeyint.recipeapp.ui.components.AppTextField
import com.yeyint.recipeapp.ui.components.PasswordTextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * Login screen. Successful login flips the shared AuthState, which switches
 * the root navigation to the main app automatically.
 */
@Composable
fun LoginScreen(
    onRegisterClick: () -> Unit,
    viewModel: LoginContract = koinViewModel<LoginViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val error = (uiState as? LoginUiState.Error)?.error

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome back 👋", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Log in to manage your recipes, pantry and shopping list.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        AppTextField(
            value = email,
            onValueChange = { email = it; viewModel.consumeError() },
            label = "Email",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        PasswordTextField(
            value = password,
            onValueChange = { password = it; viewModel.consumeError() },
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                error.displayMessage(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(24.dp))
        AppButton(
            text = "Log in",
            loading = uiState is LoginUiState.Loading,
            onClick = { viewModel.login(email, password) },
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRegisterClick) { Text("Create one") }
        }
    }
}

private class FakeLoginContract(state: LoginUiState = LoginUiState.Idle) : LoginContract {
    override val uiState: StateFlow<LoginUiState> = MutableStateFlow(state)
    override fun login(email: String, password: String) = Unit
    override fun consumeError() = Unit
}

@Preview
@Composable
private fun LoginScreenIdlePreview() {
    MaterialTheme {
        Surface {
            LoginScreen(onRegisterClick = {}, viewModel = FakeLoginContract())
        }
    }
}

@Preview
@Composable
private fun LoginScreenLoadingPreview() {
    MaterialTheme {
        Surface {
            LoginScreen(onRegisterClick = {}, viewModel = FakeLoginContract(LoginUiState.Loading))
        }
    }
}

@Preview
@Composable
private fun LoginScreenErrorPreview() {
    MaterialTheme {
        Surface {
            LoginScreen(
                onRegisterClick = {},
                viewModel = FakeLoginContract(LoginUiState.Error(AppError.Validation("Invalid email or password"))),
            )
        }
    }
}
