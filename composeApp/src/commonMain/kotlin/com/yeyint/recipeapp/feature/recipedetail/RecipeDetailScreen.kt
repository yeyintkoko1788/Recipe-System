package com.yeyint.recipeapp.feature.recipedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.material3.Surface
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.RecipeDetail
import com.yeyint.recipeapp.shared.domain.model.RecipeIngredientLine
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.ui.components.DifficultyBadge
import com.yeyint.recipeapp.ui.components.ErrorView
import com.yeyint.recipeapp.ui.components.LoadingView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: RecipeDetailContract = koinViewModel<RecipeDetailViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) { viewModel.load(recipeId) }
    LaunchedEffect(uiState) {
        if ((uiState as? RecipeDetailUiState.Data)?.deleted == true) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val data = uiState as? RecipeDetailUiState.Data
                    if (data?.canModify == true) {
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit recipe") }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, "Delete recipe", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            RecipeDetailUiState.Loading -> LoadingView(Modifier.padding(padding))
            is RecipeDetailUiState.Error ->
                ErrorView(state.error, onRetry = { viewModel.load(recipeId) }, modifier = Modifier.padding(padding))
            is RecipeDetailUiState.Data -> {
                val recipe = state.recipe
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                ) {
                    recipe.coverImageUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = recipe.title,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(recipe.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                            DifficultyBadge(recipe.difficulty)
                        }
                        Text(
                            "by ${recipe.authorName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoChip(icon = { Icon(Icons.Outlined.Schedule, null, Modifier.width(16.dp)) },
                                text = "${recipe.cookingTimeMinutes} min")
                            InfoChip(icon = { Icon(Icons.Outlined.Restaurant, null, Modifier.width(16.dp)) },
                                text = "${recipe.servings} servings")
                        }
                        if (recipe.description.isNotBlank()) {
                            Text(recipe.description, style = MaterialTheme.typography.bodyLarge)
                        }

                        Text("Ingredients", style = MaterialTheme.typography.titleLarge)
                        recipe.ingredients.forEach { line ->
                            Row(Modifier.fillMaxWidth()) {
                                Text("•  ${line.ingredientName}", modifier = Modifier.weight(1f))
                                Text(
                                    "${line.quantity.formatQuantity()} ${line.unit}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            line.note?.let {
                                Text(
                                    "    $it",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Text("Instructions", style = MaterialTheme.typography.titleLarge)
                        recipe.instructions.forEachIndexed { index, step ->
                            Row {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(step, style = MaterialTheme.typography.bodyLarge)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete recipe?") },
                        text = { Text("\"${recipe.title}\" will be removed permanently.") },
                        confirmButton = {
                            TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Double.formatQuantity(): String =
    if (this == kotlin.math.floor(this)) toInt().toString() else toString()

private class FakeRecipeDetailContract(state: RecipeDetailUiState) : RecipeDetailContract {
    override val uiState: StateFlow<RecipeDetailUiState> = MutableStateFlow(state)
    override fun load(recipeId: String) = Unit
    override fun delete() = Unit
}

private val previewRecipeDetail = RecipeDetail(
    id = "1",
    title = "Spaghetti Carbonara",
    description = "A classic Roman pasta dish made with eggs, guanciale, and Pecorino Romano.",
    cookingTimeMinutes = 25,
    servings = 2,
    difficulty = Difficulty.MEDIUM,
    coverImageUrl = null,
    instructions = listOf(
        "Boil salted water and cook spaghetti until al dente.",
        "Fry guanciale in a pan until crispy.",
        "Whisk eggs with Pecorino Romano and black pepper.",
        "Toss hot pasta with guanciale, then off heat add egg mixture.",
    ),
    ingredients = listOf(
        RecipeIngredientLine("i1", "Spaghetti", 200.0, "g", null),
        RecipeIngredientLine("i2", "Guanciale", 100.0, "g", null),
        RecipeIngredientLine("i3", "Eggs", 3.0, "pcs", "room temperature"),
        RecipeIngredientLine("i4", "Pecorino Romano", 50.0, "g", "finely grated"),
    ),
    authorId = "u1",
    authorName = "Chef Mario",
    viewCount = 1240,
)

@Preview
@Composable
private fun RecipeDetailLoadingPreview() {
    MaterialTheme {
        Surface {
            RecipeDetailScreen(recipeId = "1", onBack = {}, onEdit = {},
                viewModel = FakeRecipeDetailContract(RecipeDetailUiState.Loading))
        }
    }
}

@Preview
@Composable
private fun RecipeDetailDataPreview() {
    MaterialTheme {
        Surface {
            RecipeDetailScreen(
                recipeId = "1", onBack = {}, onEdit = {},
                viewModel = FakeRecipeDetailContract(
                    RecipeDetailUiState.Data(recipe = previewRecipeDetail, canModify = false),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun RecipeDetailEditablePreview() {
    MaterialTheme {
        Surface {
            RecipeDetailScreen(
                recipeId = "1", onBack = {}, onEdit = {},
                viewModel = FakeRecipeDetailContract(
                    RecipeDetailUiState.Data(recipe = previewRecipeDetail, canModify = true),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun RecipeDetailErrorPreview() {
    MaterialTheme {
        Surface {
            RecipeDetailScreen(
                recipeId = "1", onBack = {}, onEdit = {},
                viewModel = FakeRecipeDetailContract(RecipeDetailUiState.Error(AppError.Network)),
            )
        }
    }
}
