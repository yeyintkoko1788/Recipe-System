package com.yeyint.recipeapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.model.User
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.domain.repository.AuthState
import com.yeyint.recipeapp.shared.domain.repository.RecipeRepository
import com.yeyint.recipeapp.shared.domain.repository.RecipeSearchFilter
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface ProfileContract {
    val uiState: StateFlow<ProfileUiState>
    fun refresh()
    fun logout()
}

data class ProfileUiState(
    val user: User? = null,
    val myRecipes: List<RecipeSummary> = emptyList(),
    val isLoadingRecipes: Boolean = true,
    val recipesError: AppError? = null,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel(), ProfileContract {

    private val _uiState = MutableStateFlow(ProfileUiState())
    override val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        val user = (authRepository.authState.value as? AuthState.LoggedIn)?.user
        _uiState.value = _uiState.value.copy(user = user, isLoadingRecipes = true, recipesError = null)
        viewModelScope.launch {
            recipeRepository.search(RecipeSearchFilter(onlyMine = true), page = 0, size = 50)
                .onSuccess { _uiState.value = _uiState.value.copy(myRecipes = it.items, isLoadingRecipes = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoadingRecipes = false, recipesError = it) }
        }
    }

    override fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
