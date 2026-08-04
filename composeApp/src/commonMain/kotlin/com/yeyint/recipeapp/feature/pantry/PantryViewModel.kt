package com.yeyint.recipeapp.feature.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.PantryItem
import com.yeyint.recipeapp.shared.domain.repository.PantryRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface PantryContract {
    val uiState: StateFlow<PantryUiState>
    fun refresh()
    fun markOutOfStock(itemId: String)
    fun restock(itemId: String, quantity: Double)
    fun updateQuantity(itemId: String, quantity: Double)
    fun remove(itemId: String)
    fun consumeError()
}

data class PantryUiState(
    val items: List<PantryItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val actionError: AppError? = null,
)

class PantryViewModel(
    private val pantryRepository: PantryRepository,
) : ViewModel(), PantryContract {

    private val _uiState = MutableStateFlow(PantryUiState())
    override val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    override fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = _uiState.value.items.isEmpty(), error = null)
        viewModelScope.launch {
            pantryRepository.items()
                .onSuccess { _uiState.value = PantryUiState(items = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it) }
        }
    }

    override fun markOutOfStock(itemId: String) = mutate { pantryRepository.markOutOfStock(itemId) }

    override fun restock(itemId: String, quantity: Double) = mutate { pantryRepository.restock(itemId, quantity) }

    override fun updateQuantity(itemId: String, quantity: Double) =
        mutate { pantryRepository.update(itemId, quantity = quantity, unit = null, isOutOfStock = null) }

    override fun remove(itemId: String) {
        viewModelScope.launch {
            pantryRepository.remove(itemId)
                .onSuccess { refresh() }
                .onFailure { _uiState.value = _uiState.value.copy(actionError = it) }
        }
    }

    override fun consumeError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }

    /** Runs a single-item mutation and swaps the updated item into the list. */
    private fun mutate(action: suspend () -> com.yeyint.recipeapp.shared.util.AppResult<PantryItem>) {
        viewModelScope.launch {
            action()
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        items = _uiState.value.items.map { if (it.id == updated.id) updated else it },
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(actionError = it) }
        }
    }
}
