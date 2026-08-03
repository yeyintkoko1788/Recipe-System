package com.yeyint.recipeapp.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.ShoppingItem
import com.yeyint.recipeapp.shared.domain.repository.ShoppingRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.displayMessage
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface ShoppingListContract {
    val uiState: StateFlow<ShoppingListUiState>
    fun refresh()
    fun addManual(name: String, quantity: Double?, unit: String?)
    fun generateFromPantry()
    fun purchase(itemId: String)
    fun remove(itemId: String)
    fun clearPurchased()
    fun consumeMessage()
}

data class ShoppingListUiState(
    val pending: List<ShoppingItem> = emptyList(),
    val purchased: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val error: AppError? = null,
    val message: String? = null,
)

class ShoppingListViewModel(
    private val shoppingRepository: ShoppingRepository,
) : ViewModel(), ShoppingListContract {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    override val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            shoppingRepository.items(includePurchased = true)
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        pending = items.filterNot { it.isPurchased },
                        purchased = items.filter { it.isPurchased },
                        isLoading = false,
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it) }
        }
    }

    override fun addManual(name: String, quantity: Double?, unit: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            shoppingRepository.addManual(name.trim(), quantity, unit?.takeIf { it.isNotBlank() })
                .onSuccess { refresh() }
                .onFailure { setMessage(it) }
        }
    }

    /** Pulls all out-of-stock pantry ingredients onto the list. */
    override fun generateFromPantry() {
        _uiState.value = _uiState.value.copy(isGenerating = true)
        viewModelScope.launch {
            shoppingRepository.generateFromPantry()
                .onSuccess { added ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        message = if (added.isEmpty()) "Nothing new to add — pantry is covered!"
                        else "Added ${added.size} item(s) from your pantry",
                    )
                    refresh()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isGenerating = false)
                    setMessage(it)
                }
        }
    }

    /** Marking purchased also restocks the linked pantry item (backend default). */
    override fun purchase(itemId: String) {
        viewModelScope.launch {
            shoppingRepository.purchase(itemId, restockPantry = true, quantity = null)
                .onSuccess { refresh() }
                .onFailure { setMessage(it) }
        }
    }

    override fun remove(itemId: String) {
        viewModelScope.launch {
            shoppingRepository.remove(itemId)
                .onSuccess { refresh() }
                .onFailure { setMessage(it) }
        }
    }

    override fun clearPurchased() {
        viewModelScope.launch {
            shoppingRepository.clearPurchased()
                .onSuccess { refresh() }
                .onFailure { setMessage(it) }
        }
    }

    override fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun setMessage(error: AppError) {
        _uiState.value = _uiState.value.copy(
            message = error.displayMessage(),
        )
    }
}
