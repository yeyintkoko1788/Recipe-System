package com.yeyint.recipeapp.feature.ingredients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.Ingredient
import com.yeyint.recipeapp.shared.domain.repository.IngredientRepository
import com.yeyint.recipeapp.shared.domain.repository.PantryRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.displayMessage
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

interface IngredientListContract {
    val uiState: StateFlow<IngredientListUiState>
    fun refresh()
    fun onQueryChange(query: String)
    fun loadMore()
    fun addToPantry(ingredient: Ingredient, quantity: Double, unit: String?)
    fun submitIngredient(name: String, category: String, unit: String)
    fun consumeMessage()
}

data class IngredientListUiState(
    val query: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val error: AppError? = null,
    val message: String? = null,
)

/**
 * Ingredient catalog browser. From here users add items to their pantry or
 * submit missing ingredients (which await admin approval).
 */
@OptIn(FlowPreview::class)
class IngredientListViewModel(
    private val ingredientRepository: IngredientRepository,
) : ViewModel(), IngredientListContract {

    // PantryRepository is injected lazily via Koin at call time to keep the
    // constructor small for tests that only exercise browsing.
    var pantryRepository: PantryRepository? = null

    private val _uiState = MutableStateFlow(IngredientListUiState())
    override val uiState: StateFlow<IngredientListUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var currentPage = 0

    init {
        viewModelScope.launch {
            queryFlow.debounce(300).distinctUntilChanged().collect { load(0) }
        }
    }

    override fun refresh() {
        if (_uiState.value.ingredients.isEmpty()) {
            load(0)
        } else {
            // Background refresh: keep showing existing list, update silently.
            viewModelScope.launch {
                ingredientRepository.list(_uiState.value.query.takeIf { it.isNotBlank() }, page = 0)
                    .onSuccess { result ->
                        currentPage = 0
                        _uiState.value = _uiState.value.copy(
                            ingredients = result.items,
                            hasMore = result.hasMore,
                            error = null,
                        )
                    }
            }
        }
    }

    override fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
    }

    override fun loadMore() {
        if (_uiState.value.hasMore) load(currentPage + 1)
    }

    override fun addToPantry(ingredient: Ingredient, quantity: Double, unit: String?) {
        val pantry = pantryRepository ?: return
        viewModelScope.launch {
            pantry.add(ingredient.id, quantity, unit)
                .onSuccess { _uiState.value = _uiState.value.copy(message = "${ingredient.name} added to pantry") }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.displayMessage()) }
        }
    }

    override fun submitIngredient(name: String, category: String, unit: String) {
        viewModelScope.launch {
            ingredientRepository.submit(name, category, unit)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        message = "Submitted \"${it.name}\" — pending approval",
                    )
                    load(0)
                }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.displayMessage()) }
        }
    }

    override fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun load(page: Int) {
        viewModelScope.launch {
            if (page == 0) _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            ingredientRepository.list(_uiState.value.query.takeIf { it.isNotBlank() }, page)
                .onSuccess { result ->
                    currentPage = page
                    val merged = if (page == 0) result.items else _uiState.value.ingredients + result.items
                    _uiState.value = _uiState.value.copy(
                        ingredients = merged, isLoading = false, hasMore = result.hasMore,
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it) }
        }
    }
}
