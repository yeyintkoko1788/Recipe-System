package com.yeyint.recipeapp.feature.recipedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.RecipeDetail
import com.yeyint.recipeapp.shared.domain.repository.AuthRepository
import com.yeyint.recipeapp.shared.domain.repository.AuthState
import com.yeyint.recipeapp.shared.domain.model.UserRole
import com.yeyint.recipeapp.shared.domain.repository.RecipeRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface RecipeDetailContract {
    val uiState: StateFlow<RecipeDetailUiState>
    fun load(recipeId: String)
    fun delete()
}

sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState
    data class Data(
        val recipe: RecipeDetail,
        /** Author or admin — controls edit/delete visibility. */
        val canModify: Boolean,
        val isDeleting: Boolean = false,
        val deleted: Boolean = false,
        val actionError: AppError? = null,
    ) : RecipeDetailUiState
    data class Error(val error: AppError) : RecipeDetailUiState
}

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val authRepository: AuthRepository,
) : ViewModel(), RecipeDetailContract {

    private val _uiState = MutableStateFlow<RecipeDetailUiState>(RecipeDetailUiState.Loading)
    override val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private var recipeId: String? = null

    override fun load(recipeId: String) {
        this.recipeId = recipeId
        _uiState.value = RecipeDetailUiState.Loading
        viewModelScope.launch {
            recipeRepository.detail(recipeId)
                .onSuccess { recipe ->
                    val user = (authRepository.authState.value as? AuthState.LoggedIn)?.user
                    val canModify = user != null && (user.id == recipe.authorId || user.role == UserRole.ADMIN)
                    _uiState.value = RecipeDetailUiState.Data(recipe, canModify)
                }
                .onFailure { _uiState.value = RecipeDetailUiState.Error(it) }
        }
    }

    override fun delete() {
        val state = _uiState.value as? RecipeDetailUiState.Data ?: return
        val id = recipeId ?: return
        _uiState.value = state.copy(isDeleting = true, actionError = null)
        viewModelScope.launch {
            recipeRepository.delete(id)
                .onSuccess { _uiState.value = state.copy(isDeleting = false, deleted = true) }
                .onFailure { _uiState.value = state.copy(isDeleting = false, actionError = it) }
        }
    }
}
