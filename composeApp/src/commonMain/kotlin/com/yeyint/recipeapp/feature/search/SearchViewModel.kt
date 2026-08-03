package com.yeyint.recipeapp.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.repository.RecipeRepository
import com.yeyint.recipeapp.shared.domain.repository.RecipeSearchFilter
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

interface SearchContract {
    val uiState: StateFlow<SearchUiState>
    fun onQueryChange(query: String)
    fun setDifficulty(difficulty: Difficulty?)
    fun setSort(sortBy: String)
    fun loadMore()
}

data class SearchUiState(
    val query: String = "",
    val difficulty: Difficulty? = null,
    val sortBy: String = "createdAt",
    val results: List<RecipeSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val hasSearched: Boolean = false,
    val error: AppError? = null,
)

/** Debounced recipe search with difficulty filter and sort options. */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val recipeRepository: RecipeRepository,
) : ViewModel(), SearchContract {

    private val _uiState = MutableStateFlow(SearchUiState())
    override val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private var currentPage = 0

    init {
        viewModelScope.launch {
            queryFlow.debounce(350).distinctUntilChanged().collect { search(page = 0) }
        }
    }

    override fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
    }

    override fun setDifficulty(difficulty: Difficulty?) {
        _uiState.value = _uiState.value.copy(difficulty = difficulty)
        search(page = 0)
    }

    override fun setSort(sortBy: String) {
        _uiState.value = _uiState.value.copy(sortBy = sortBy)
        search(page = 0)
    }

    override fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        search(page = currentPage + 1)
    }

    private fun search(page: Int) {
        searchJob?.cancel()
        val state = _uiState.value
        _uiState.value = state.copy(
            isLoading = page == 0,
            isLoadingMore = page > 0,
            error = null,
        )
        searchJob = viewModelScope.launch {
            recipeRepository.search(
                RecipeSearchFilter(
                    query = state.query.takeIf { it.isNotBlank() },
                    difficulty = state.difficulty,
                    sortBy = state.sortBy,
                    ascending = state.sortBy == "title",
                ),
                page = page,
            )
                .onSuccess { result ->
                    currentPage = page
                    val merged = if (page == 0) result.items else _uiState.value.results + result.items
                    _uiState.value = _uiState.value.copy(
                        results = merged,
                        isLoading = false, isLoadingMore = false,
                        hasMore = result.hasMore, hasSearched = true,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false, error = it)
                }
        }
    }
}
