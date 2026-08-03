package com.yeyint.recipeapp

import app.cash.turbine.test
import com.yeyint.recipeapp.feature.auth.LoginUiState
import com.yeyint.recipeapp.feature.auth.LoginViewModel
import com.yeyint.recipeapp.shared.domain.model.User
import com.yeyint.recipeapp.shared.domain.model.UserRole
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.domain.repository.AuthState
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Fake auth repository driven by a queued result. */
private class FakeAuthRepository : AuthRepository {
    var nextResult: AppResult<User> = AppResult.Failure(AppError.Unknown())
    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    override val authState: StateFlow<AuthState> = _authState

    override suspend fun restoreSession() = Unit
    override suspend fun login(email: String, password: String): AppResult<User> = nextResult
    override suspend fun register(name: String, email: String, password: String): AppResult<User> = nextResult
    override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
    override fun onSessionExpired() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankInputFailsFastWithoutHittingTheRepository() = runTest(dispatcher) {
        val viewModel = LoginViewModel(repository)
        viewModel.login("", "")
        assertIs<LoginUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun successfulLoginEmitsLoadingThenSuccess() = runTest(dispatcher) {
        val user = User("u1", "Ye Yint", "ye@example.com", UserRole.USER)
        repository.nextResult = AppResult.Success(user)
        val viewModel = LoginViewModel(repository)

        viewModel.uiState.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            viewModel.login("ye@example.com", "password1")
            assertEquals(LoginUiState.Loading, awaitItem())
            assertEquals(LoginUiState.Success(user), awaitItem())
        }
    }

    @Test
    fun sessionExpiredDuringLoginIsShownAsInvalidCredentials() = runTest(dispatcher) {
        repository.nextResult = AppResult.Failure(AppError.SessionExpired)
        val viewModel = LoginViewModel(repository)

        viewModel.uiState.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            viewModel.login("ye@example.com", "wrong")
            assertEquals(LoginUiState.Loading, awaitItem())
            val error = awaitItem()
            assertIs<LoginUiState.Error>(error)
            assertIs<AppError.Validation>(error.error)
        }
    }

    @Test
    fun consumeErrorReturnsToIdle() = runTest(dispatcher) {
        repository.nextResult = AppResult.Failure(AppError.Network)
        val viewModel = LoginViewModel(repository)

        viewModel.login("ye@example.com", "password1")
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<LoginUiState.Error>(viewModel.uiState.value)

        viewModel.consumeError()
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }
}
