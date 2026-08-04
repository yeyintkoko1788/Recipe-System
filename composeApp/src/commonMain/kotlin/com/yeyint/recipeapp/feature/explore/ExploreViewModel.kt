package com.yeyint.recipeapp.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.repository.ExploreRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ExploreTab { POPULAR, NEW, RECOMMENDED }

interface ExploreContract {
    val uiState: StateFlow<ExploreUiState>
    fun selectTab(tab: ExploreTab)
    fun loadMore()
    fun refresh()
}

data class ExploreUiState(
    val tab: ExploreTab = ExploreTab.POPULAR,
    val recipes: List<RecipeSummary> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: AppError? = null,
)

/** Paged explore feeds with tab switching. */
class ExploreViewModel(
    private val exploreRepository: ExploreRepository,
) : ViewModel(), ExploreContract {

    private val _uiState = MutableStateFlow(ExploreUiState())
    override val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var currentPage = 0

    override fun selectTab(tab: ExploreTab) {
        if (tab == _uiState.value.tab) return
        _uiState.value = ExploreUiState(tab = tab)
        currentPage = 0
        load(page = 0)
    }

    override fun refresh() {
        currentPage = 0
        _uiState.value = _uiState.value.copy(
            isLoading = _uiState.value.recipes.isEmpty(),
            error = null,
        )
        load(page = 0)
    }

    override fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.value = state.copy(isLoadingMore = true)
        load(page = currentPage + 1)
    }

    private fun load(page: Int) {
        viewModelScope.launch {
            when (_uiState.value.tab) {
                ExploreTab.POPULAR -> exploreRepository.popular(page, PAGE_SIZE).handlePaged(page)
                ExploreTab.NEW -> exploreRepository.newest(page, PAGE_SIZE).handlePaged(page)
                ExploreTab.RECOMMENDED -> exploreRepository.recommended(limit = 30)
                    .onSuccess { recipes ->
                        _uiState.value = _uiState.value.copy(
                            recipes = recipes, isLoading = false, isLoadingMore = false, hasMore = false,
                        )
                    }
                    .onFailure { setError(it) }
            }
        }
    }

    private fun com.yeyint.recipeapp.shared.util.AppResult<com.yeyint.recipeapp.shared.domain.model.Page<RecipeSummary>>.handlePaged(page: Int) {
        onSuccess { result ->
            currentPage = page
            val merged = if (page == 0) result.items else _uiState.value.recipes + result.items
            _uiState.value = _uiState.value.copy(
                recipes = merged, isLoading = false, isLoadingMore = false, hasMore = result.hasMore,
            )
        }
        onFailure { setError(it) }
    }

    private fun setError(error: AppError) {
        _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false, error = error)
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
