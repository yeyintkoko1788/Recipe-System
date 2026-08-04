package com.yeyint.recipeapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.domain.repository.AuthState
import com.yeyint.recipeapp.shared.domain.repository.ExploreRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.AppResult
import com.yeyint.recipeapp.shared.util.getOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface HomeContract {
    val uiState: StateFlow<HomeUiState>
    fun refresh()
    fun logoutName(): String
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Data(
        val userName: String,
        val recommended: List<RecipeSummary>,
        val popular: List<RecipeSummary>,
        val newest: List<RecipeSummary>,
    ) : HomeUiState
    data class Error(val error: AppError) : HomeUiState
}

class HomeViewModel(
    private val exploreRepository: ExploreRepository,
    private val authRepository: AuthRepository,
) : ViewModel(), HomeContract {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    override val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    override fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            // Keep showing existing data while refreshing; only show spinner on first load or after an error.
            if (_uiState.value !is HomeUiState.Data) {
                _uiState.value = HomeUiState.Loading
            }

            // Fetch the three feeds concurrently.
            val recommendedDeferred = async { exploreRepository.recommended(limit = 10) }
            val popularDeferred = async { exploreRepository.popular(page = 0, size = 10) }
            val newestDeferred = async { exploreRepository.newest(page = 0, size = 10) }

            val recommended = recommendedDeferred.await()
            val popular = popularDeferred.await()
            val newest = newestDeferred.await()

            // Only fail the whole screen when every feed failed.
            val firstFailure = listOf(recommended, popular, newest)
                .filterIsInstance<AppResult.Failure>()
                .firstOrNull()
            if (firstFailure != null && recommended !is AppResult.Success &&
                popular !is AppResult.Success && newest !is AppResult.Success
            ) {
                _uiState.value = HomeUiState.Error(firstFailure.error)
                return@launch
            }

            _uiState.value = HomeUiState.Data(
                userName = logoutName(),
                recommended = recommended.getOrNull().orEmpty(),
                popular = popular.getOrNull()?.items.orEmpty(),
                newest = newest.getOrNull()?.items.orEmpty(),
            )
        }
    }

    override fun logoutName(): String =
        (authRepository.authState.value as? AuthState.LoggedIn)?.user?.name ?: ""
}
