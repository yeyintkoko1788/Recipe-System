package com.yeyint.recipeapp.feature.pantry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.yeyint.recipeapp.shared.domain.model.Ingredient
import com.yeyint.recipeapp.shared.domain.model.IngredientStatus
import com.yeyint.recipeapp.shared.domain.model.PantryItem
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.displayMessage
import com.yeyint.recipeapp.theme.AppColors
import com.yeyint.recipeapp.ui.components.AppButton
import com.yeyint.recipeapp.ui.components.EmptyView
import com.yeyint.recipeapp.ui.components.ErrorView
import com.yeyint.recipeapp.ui.components.LoadingView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    onBrowseIngredients: () -> Unit,
    viewModel: PantryContract = koinViewModel<PantryViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var restockTarget by remember { mutableStateOf<PantryItem?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it.displayMessage())
            viewModel.consumeError()
        }
    }

    Scaffold(
        // MainScaffold already applies the window insets and passes them
        // to this screen; re-applying them here would double the top
        // padding (very visible on iOS, where the safe area is ~59pt).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val categories = uiState.items.map { it.ingredient.category }.distinct().sorted()
        val activeCategory = category?.takeIf { it in categories }
        val visibleItems = uiState.items.filter {
            activeCategory == null || it.ingredient.category == activeCategory
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Pantry", style = MaterialTheme.typography.headlineLarge)
                    if (uiState.items.isNotEmpty()) {
                        Text(
                            "${uiState.items.count { !it.isOutOfStock }} ingredients on hand",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = onBrowseIngredients,
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add ingredient to pantry",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(uiState.error!!, onRetry = viewModel::refresh)
                uiState.items.isEmpty() -> EmptyView(
                    "Your pantry is empty",
                    "Add ingredients you have at home to get recipe recommendations.",
                )
                else -> {
                    if (categories.size > 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                PantryChip("All ${uiState.items.size}", activeCategory == null) { category = null }
                            }
                            items(categories) { name ->
                                PantryChip(name, activeCategory == name) { category = name }
                            }
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(visibleItems, key = { it.id }) { item ->
                            PantryItemCard(
                                item = item,
                                onMarkEmpty = { viewModel.markOutOfStock(item.id) },
                                onRestock = { restockTarget = item },
                                onRemove = { viewModel.remove(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    restockTarget?.let { item ->
        var quantity by remember(item.id) { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { restockTarget = null }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Restock ${item.ingredient.name}", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (${item.unit})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppButton(
                    text = "Restock",
                    enabled = (quantity.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = {
                        viewModel.restock(item.id, quantity.toDoubleOrNull() ?: 0.0)
                        restockTarget = null
                    },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private class FakePantryContract(state: PantryUiState) : PantryContract {
    override val uiState: StateFlow<PantryUiState> = MutableStateFlow(state)
    override fun refresh() = Unit
    override fun markOutOfStock(itemId: String) = Unit
    override fun restock(itemId: String, quantity: Double) = Unit
    override fun updateQuantity(itemId: String, quantity: Double) = Unit
    override fun remove(itemId: String) = Unit
    override fun consumeError() = Unit
}

private fun fakeIngredient(id: String, name: String) =
    Ingredient(id, name, "Produce", "kg", null, IngredientStatus.APPROVED)

private val previewPantryItems = listOf(
    PantryItem("p1", fakeIngredient("i1", "Tomatoes"), 2.0, "kg", false),
    PantryItem("p2", fakeIngredient("i2", "Flour"), 0.0, "kg", true),
    PantryItem("p3", fakeIngredient("i3", "Olive Oil"), 0.5, "L", false),
)

@Preview
@Composable
private fun PantryScreenLoadingPreview() {
    MaterialTheme {
        Surface {
            PantryScreen(onBrowseIngredients = {},
                viewModel = FakePantryContract(PantryUiState(isLoading = true)))
        }
    }
}

@Preview
@Composable
private fun PantryScreenDataPreview() {
    MaterialTheme {
        Surface {
            PantryScreen(
                onBrowseIngredients = {},
                viewModel = FakePantryContract(PantryUiState(items = previewPantryItems, isLoading = false)),
            )
        }
    }
}

@Preview
@Composable
private fun PantryScreenEmptyPreview() {
    MaterialTheme {
        Surface {
            PantryScreen(
                onBrowseIngredients = {},
                viewModel = FakePantryContract(PantryUiState(isLoading = false)),
            )
        }
    }
}

@Preview
@Composable
private fun PantryScreenErrorPreview() {
    MaterialTheme {
        Surface {
            PantryScreen(
                onBrowseIngredients = {},
                viewModel = FakePantryContract(PantryUiState(isLoading = false, error = AppError.Network)),
            )
        }
    }
}

@Composable
private fun PantryItemCard(
    item: PantryItem,
    onMarkEmpty: () -> Unit,
    onRestock: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)) {
            Text(
                item.ingredient.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            Spacer(Modifier.height(2.dp))
            if (item.isOutOfStock) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = AppColors.Error.copy(alpha = 0.12f),
                    contentColor = AppColors.Error,
                ) {
                    Text(
                        "Out of stock",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            } else {
                Text(
                    "${item.quantity} ${item.unit}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isOutOfStock) {
                    TextButton(onClick = onRestock) { Text("Restock") }
                } else {
                    TextButton(onClick = onMarkEmpty) { Text("Mark empty") }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = CircleShape,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
