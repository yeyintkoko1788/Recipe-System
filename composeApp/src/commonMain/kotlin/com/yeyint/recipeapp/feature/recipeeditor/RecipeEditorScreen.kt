package com.yeyint.recipeapp.feature.recipeeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.Ingredient
import com.yeyint.recipeapp.shared.util.displayMessage
import com.yeyint.recipeapp.ui.components.AppButton
import com.yeyint.recipeapp.ui.components.AppTextField
import com.yeyint.recipeapp.ui.components.LoadingView
import org.koin.compose.viewmodel.koinViewModel

/** Create/edit recipe form with an ingredient-picker bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    recipeId: String?,
    onDone: () -> Unit,
    viewModel: RecipeEditorContract = koinViewModel<RecipeEditorViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showIngredientPicker by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) { viewModel.start(recipeId) }
    LaunchedEffect(uiState.saved) { if (uiState.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit recipe" else "New recipe") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            LoadingView(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AppTextField(uiState.title, viewModel::setTitle, "Title", errorMessage = uiState.fieldErrors["title"])
                }
                item {
                    AppTextField(
                        uiState.description, viewModel::setDescription, "Description",
                        singleLine = false, minLines = 3,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppTextField(
                            uiState.cookingTime, viewModel::setCookingTime, "Minutes",
                            modifier = Modifier.weight(1f),
                            errorMessage = uiState.fieldErrors["cookingTime"],
                            keyboardType = KeyboardType.Number,
                        )
                        AppTextField(
                            uiState.servings, viewModel::setServings, "Servings",
                            modifier = Modifier.weight(1f),
                            errorMessage = uiState.fieldErrors["servings"],
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Difficulty.entries.forEach { difficulty ->
                            FilterChip(
                                selected = uiState.difficulty == difficulty,
                                onClick = { viewModel.setDifficulty(difficulty) },
                                label = { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                }
                item {
                    AppTextField(uiState.coverImageUrl, viewModel::setCoverImageUrl, "Cover image URL (optional)")
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ingredients", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showIngredientPicker = true }) {
                            Icon(Icons.Filled.Add, null)
                            Text("Add")
                        }
                    }
                    uiState.fieldErrors["ingredients"]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                }
                items(uiState.ingredients, key = { it.ingredientId }) { line ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${line.ingredientName} — ${line.quantity} ${line.unit}", modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeIngredient(line.ingredientId) }) {
                            Icon(Icons.Filled.Close, "Remove ingredient")
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Steps", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::addStep) {
                            Icon(Icons.Filled.Add, null)
                            Text("Add step")
                        }
                    }
                    uiState.fieldErrors["instructions"]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                }
                items(uiState.steps.size) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}.", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = uiState.steps[index],
                            onValueChange = { viewModel.updateStep(index, it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Describe this step…") },
                        )
                        IconButton(onClick = { viewModel.removeStep(index) }) {
                            Icon(Icons.Filled.Close, "Remove step")
                        }
                    }
                }

                item {
                    uiState.error?.let {
                        Text(it.displayMessage(), color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    AppButton(
                        text = if (uiState.isEditMode) "Save changes" else "Create recipe",
                        loading = uiState.isSaving,
                        onClick = viewModel::save,
                    )
                }
            }
        }

        if (showIngredientPicker) {
            IngredientPickerSheet(
                suggestions = uiState.ingredientSuggestions,
                onSearch = viewModel::searchIngredients,
                onPick = { ingredient, quantity, unit ->
                    viewModel.addIngredient(ingredient, quantity, unit)
                    showIngredientPicker = false
                },
                onDismiss = { showIngredientPicker = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientPickerSheet(
    suggestions: List<Ingredient>,
    onSearch: (String) -> Unit,
    onPick: (Ingredient, Double, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Ingredient?>(null) }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val current = selected
            if (current == null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    placeholder = { Text("Search ingredients…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(suggestions, key = { it.id }) { ingredient ->
                        TextButton(
                            onClick = { selected = ingredient; unit = ingredient.defaultUnit },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${ingredient.name}  ·  ${ingredient.category.lowercase()}", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            } else {
                Text(current.name, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                AppButton(
                    text = "Add ingredient",
                    enabled = (quantity.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = { onPick(current, quantity.toDoubleOrNull() ?: 0.0, unit) },
                )
                TextButton(onClick = { selected = null }) { Text("Back to search") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
