package com.yeyint.recipeapp.feature.ingredients

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.yeyint.recipeapp.shared.domain.model.Ingredient
import com.yeyint.recipeapp.shared.domain.model.IngredientStatus
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.ui.components.AppButton
import com.yeyint.recipeapp.ui.components.EmptyView
import com.yeyint.recipeapp.ui.components.ErrorView
import com.yeyint.recipeapp.ui.components.LoadingView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientListScreen(
    onBack: () -> Unit,
    viewModel: IngredientListContract = koinViewModel<IngredientListViewModel>(),
) {

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pantryTarget by remember { mutableStateOf<Ingredient?>(null) }
    var showSubmitSheet by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        // MainScaffold already applies the window insets and passes them
        // to this screen; re-applying them here would double the top
        // padding (very visible on iOS, where the safe area is ~59pt).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ingredients") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showSubmitSheet = true }) {
                        Icon(Icons.Filled.Add, null)
                        Text("Suggest")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search ingredients…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(uiState.error!!)
                uiState.ingredients.isEmpty() -> EmptyView("No ingredients found", "Try suggesting a new one.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(uiState.ingredients, key = { it.id }) { ingredient ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(ingredient.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${ingredient.category.lowercase()} · ${ingredient.defaultUnit}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { pantryTarget = ingredient }) { Text("Add to pantry") }
                        }
                    }
                    if (uiState.hasMore) {
                        item { LaunchedEffect(uiState.ingredients.size) { viewModel.loadMore() } }
                    }
                }
            }
        }
    }

    pantryTarget?.let { ingredient ->
        var quantity by remember(ingredient.id) { mutableStateOf("") }
        var unit by remember(ingredient.id) { mutableStateOf(ingredient.defaultUnit) }
        ModalBottomSheet(onDismissRequest = { pantryTarget = null }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add ${ingredient.name} to pantry", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantity, onValueChange = { quantity = it },
                        label = { Text("Quantity") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = unit, onValueChange = { unit = it },
                        label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                AppButton(
                    text = "Add",
                    enabled = (quantity.toDoubleOrNull() ?: -1.0) >= 0.0,
                    onClick = {
                        viewModel.addToPantry(ingredient, quantity.toDoubleOrNull() ?: 0.0, unit)
                        pantryTarget = null
                    },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showSubmitSheet) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showSubmitSheet = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Suggest a new ingredient", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Submissions are reviewed by an admin before they appear in the catalog.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = category, onValueChange = { category = it },
                        label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = unit, onValueChange = { unit = it },
                        label = { Text("Default unit") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                AppButton(
                    text = "Submit",
                    enabled = name.isNotBlank() && category.isNotBlank() && unit.isNotBlank(),
                    onClick = {
                        viewModel.submitIngredient(name, category, unit)
                        showSubmitSheet = false
                    },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private class FakeIngredientListContract(state: IngredientListUiState = IngredientListUiState()) : IngredientListContract {
    override val uiState: StateFlow<IngredientListUiState> = MutableStateFlow(state)
    override fun refresh() = Unit
    override fun onQueryChange(query: String) = Unit
    override fun loadMore() = Unit
    override fun addToPantry(ingredient: Ingredient, quantity: Double, unit: String?) = Unit
    override fun submitIngredient(name: String, category: String, unit: String) = Unit
    override fun consumeMessage() = Unit
}

private fun fakeIngredient(id: String, name: String, category: String = "Produce") =
    Ingredient(id, name, category, "kg", null, IngredientStatus.APPROVED)

@Preview
@Composable
private fun IngredientListLoadingPreview() {
    MaterialTheme {
        Surface {
            IngredientListScreen(onBack = {}, viewModel = FakeIngredientListContract())
        }
    }
}

@Preview
@Composable
private fun IngredientListDataPreview() {
    MaterialTheme {
        Surface {
            IngredientListScreen(
                onBack = {},
                viewModel = FakeIngredientListContract(
                    IngredientListUiState(
                        isLoading = false,
                        ingredients = listOf(
                            fakeIngredient("i1", "Tomatoes"),
                            fakeIngredient("i2", "Flour", "Grain"),
                            fakeIngredient("i3", "Olive Oil", "Oil"),
                            fakeIngredient("i4", "Eggs", "Dairy"),
                        ),
                    ),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun IngredientListEmptyPreview() {
    MaterialTheme {
        Surface {
            IngredientListScreen(
                onBack = {},
                viewModel = FakeIngredientListContract(IngredientListUiState(isLoading = false)),
            )
        }
    }
}

@Preview
@Composable
private fun IngredientListErrorPreview() {
    MaterialTheme {
        Surface {
            IngredientListScreen(
                onBack = {},
                viewModel = FakeIngredientListContract(
                    IngredientListUiState(isLoading = false, error = AppError.Network),
                ),
            )
        }
    }
}
