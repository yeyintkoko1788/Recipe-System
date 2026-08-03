package com.yeyint.recipeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yeyint.recipeapp.feature.auth.LoginScreen
import com.yeyint.recipeapp.feature.auth.RegisterScreen
import com.yeyint.recipeapp.navigation.MainScaffold
import com.yeyint.recipeapp.navigation.Route
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.domain.repository.AuthState
import com.yeyint.recipeapp.shared.domain.repository.SettingsRepository
import com.yeyint.recipeapp.theme.RecipeAppTheme
import com.yeyint.recipeapp.ui.components.LoadingView
import org.koin.compose.koinInject

/**
 * Application root: applies the theme and switches between the auth flow and
 * the main app based on the observed [AuthState].
 */
@Composable
fun App() {
    val settingsRepository = koinInject<SettingsRepository>()
    val authRepository = koinInject<AuthRepository>()

    val darkMode by settingsRepository.darkMode.collectAsState(initial = null)
    val authState by authRepository.authState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { authRepository.restoreSession() }

    RecipeAppTheme(darkModeOverride = darkMode) {
        when (authState) {
            AuthState.Unknown -> LoadingView()
            AuthState.LoggedOut -> AuthNavigation()
            is AuthState.LoggedIn -> MainScaffold()
        }
    }
}

@Composable
private fun AuthNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.Login) {
        composable<Route.Login> {
            LoginScreen(onRegisterClick = { navController.navigate(Route.Register) })
        }
        composable<Route.Register> {
            RegisterScreen(onBackToLogin = { navController.popBackStack() })
        }
    }
}
