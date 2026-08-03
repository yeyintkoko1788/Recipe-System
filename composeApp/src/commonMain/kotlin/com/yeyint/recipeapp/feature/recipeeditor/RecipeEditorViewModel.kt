package com.yeyint.recipeapp.feature.recipeeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.Ingredient
import com.yeyint.recipeapp.shared.domain.model.RecipeDraft
import com.yeyint.recipeapp.shared.domain.repository.IngredientRepository
import com.yeyint.recipeapp.shared.domain.repository.RecipeRepository
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.onFailure
import com.yeyint.recipeapp.shared.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface RecipeEditorContract {
    val uiState: StateFlow<RecipeEditorUiState>
    fun start(recipeId: String?)
    fun setTitle(value: String)
    fun setDescription(value: String)
    fun setCookingTime(value: String)
    fun setServings(value: String)
    fun setDifficulty(value: Difficulty)
    fun setCoverImageUrl(value: String)
    fun searchIngredients(query: String)
    fun addIngredient(ingredient: Ingredient, quantity: Double, unit: String)
    fun removeIngredient(ingredientId: String)
    fun addStep()
    fun updateStep(index: Int, value: String)
    fun removeStep(index: Int)
    fun save()
}

data class RecipeEditorUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val cookingTime: String = "",
    val servings: String = "",
    val difficulty: Difficulty = Difficulty.EASY,
    val coverImageUrl: String = "",
    val steps: List<String> = listOf(""),
    val ingredients: List<RecipeDraft.DraftIngredient> = emptyList(),
    val ingredientSuggestions: List<Ingredient> = emptyList(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

/** Shared ViewModel for both create and edit flows. */
class RecipeEditorViewModel(
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
) : ViewModel(), RecipeEditorContract {

    private val _uiState = MutableStateFlow(RecipeEditorUiState())
    override val uiState: StateFlow<RecipeEditorUiState> = _uiState.asStateFlow()

    private var editingId: String? = null
    private var started = false

    override fun start(recipeId: String?) {
        if (started) return
        started = true
        editingId = recipeId
        searchIngredients("")
        if (recipeId == null) return

        _uiState.value = _uiState.value.copy(isEditMode = true, isLoading = true)
        viewModelScope.launch {
            recipeRepository.detail(recipeId)
                .onSuccess { recipe ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        title = recipe.title,
                        description = recipe.description,
                        cookingTime = recipe.cookingTimeMinutes.toString(),
                        servings = recipe.servings.toString(),
                        difficulty = recipe.difficulty,
                        coverImageUrl = recipe.coverImageUrl.orEmpty(),
                        steps = recipe.instructions.ifEmpty { listOf("") },
                        ingredients = recipe.ingredients.map {
                            RecipeDraft.DraftIngredient(it.ingredientId, it.ingredientName, it.quantity, it.unit, it.note)
                        },
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it) }
        }
    }

    override fun setTitle(value: String) = update { copy(title = value, fieldErrors = fieldErrors - "title") }
    override fun setDescription(value: String) = update { copy(description = value) }
    override fun setCookingTime(value: String) =
        update { copy(cookingTime = value.filter(Char::isDigit), fieldErrors = fieldErrors - "cookingTime") }
    override fun setServings(value: String) =
        update { copy(servings = value.filter(Char::isDigit), fieldErrors = fieldErrors - "servings") }
    override fun setDifficulty(value: Difficulty) = update { copy(difficulty = value) }
    override fun setCoverImageUrl(value: String) = update { copy(coverImageUrl = value) }

    override fun searchIngredients(query: String) {
        viewModelScope.launch {
            ingredientRepository.list(query.takeIf { it.isNotBlank() }, page = 0, size = 30)
                .onSuccess { update { copy(ingredientSuggestions = it.items) } }
        }
    }

    override fun addIngredient(ingredient: Ingredient, quantity: Double, unit: String) = update {
        if (ingredients.any { it.ingredientId == ingredient.id }) this
        else copy(
            ingredients = ingredients + RecipeDraft.DraftIngredient(
                ingredientId = ingredient.id,
                ingredientName = ingredient.name,
                quantity = quantity,
                unit = unit.ifBlank { ingredient.defaultUnit },
            ),
            fieldErrors = fieldErrors - "ingredients",
        )
    }

    override fun removeIngredient(ingredientId: String) =
        update { copy(ingredients = ingredients.filterNot { it.ingredientId == ingredientId }) }

    override fun addStep() = update { copy(steps = steps + "") }

    override fun updateStep(index: Int, value: String) = update {
        copy(steps = steps.mapIndexed { i, step -> if (i == index) value else step }, fieldErrors = fieldErrors - "instructions")
    }

    override fun removeStep(index: Int) = update {
        if (steps.size <= 1) this else copy(steps = steps.filterIndexed { i, _ -> i != index })
    }

    override fun save() {
        val state = _uiState.value
        val errors = buildMap {
            if (state.title.trim().length < 3) put("title", "Title must be at least 3 characters")
            if ((state.cookingTime.toIntOrNull() ?: 0) <= 0) put("cookingTime", "Enter the cooking time")
            if ((state.servings.toIntOrNull() ?: 0) <= 0) put("servings", "Enter the servings")
            if (state.steps.none { it.isNotBlank() }) put("instructions", "Add at least one step")
            if (state.ingredients.isEmpty()) put("ingredients", "Add at least one ingredient")
        }
        if (errors.isNotEmpty()) {
            update { copy(fieldErrors = errors) }
            return
        }

        val draft = RecipeDraft(
            title = state.title.trim(),
            description = state.description.trim(),
            cookingTimeMinutes = state.cookingTime.toInt(),
            servings = state.servings.toInt(),
            difficulty = state.difficulty,
            coverImageUrl = state.coverImageUrl.trim().takeIf { it.isNotEmpty() },
            instructions = state.steps.filter { it.isNotBlank() },
            ingredients = state.ingredients,
        )

        update { copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = editingId?.let { recipeRepository.update(it, draft) } ?: recipeRepository.create(draft)
            result
                .onSuccess { update { copy(isSaving = false, saved = true) } }
                .onFailure { failure ->
                    update {
                        copy(
                            isSaving = false,
                            error = failure,
                            fieldErrors = (failure as? AppError.Validation)?.fields ?: fieldErrors,
                        )
                    }
                }
        }
    }

    private inline fun update(block: RecipeEditorUiState.() -> RecipeEditorUiState) {
        _uiState.value = _uiState.value.block()
    }
}
