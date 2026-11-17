package com.yeyint.recipeapp.feature.recipedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.RecipeDetail
import com.yeyint.recipeapp.shared.domain.model.RecipeIngredientLine
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.ui.components.ErrorView
import com.yeyint.recipeapp.ui.components.LoadingView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

private const val TAB_INGREDIENTS = 0
private const val TAB_STEPS = 1

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
        // MainScaffold already applies the window insets and passes them
        // to this screen; re-applying them here would double the top
        // padding (very visible on iOS, where the safe area is ~59pt).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (val state = uiState) {
            RecipeDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                LoadingView()
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, Modifier.padding(12.dp))
            }
            is RecipeDetailUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                ErrorView(state.error, onRetry = { viewModel.load(recipeId) })
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack, Modifier.padding(12.dp))
            }
            is RecipeDetailUiState.Data -> {
                val recipe = state.recipe
                var tab by rememberSaveable { mutableStateOf(TAB_INGREDIENTS) }

                Box(Modifier.fillMaxSize().padding(padding)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (recipe.coverImageUrl != null) {
                                AsyncImage(
                                    model = recipe.coverImageUrl,
                                    contentDescription = recipe.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Restaurant, null,
                                    modifier = Modifier.align(Alignment.Center).size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth().offset(y = (-24).dp),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(recipe.title, style = MaterialTheme.typography.headlineMedium)
                                    Text(
                                        "${recipe.cookingTimeMinutes} min · ${recipe.servings} servings · " +
                                            "${recipe.difficulty.label()} · by ${recipe.authorName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (recipe.description.isNotBlank()) {
                                        Text(recipe.description, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }

                                DetailTabs(selected = tab, onSelect = { tab = it })

                                if (tab == TAB_INGREDIENTS) {
                                    IngredientList(recipe.ingredients)
                                } else {
                                    StepList(recipe.instructions)
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                        Spacer(Modifier.weight(1f))
                        if (state.canModify) {
                            RoundIconButton(Icons.Filled.Edit, "Edit recipe", onEdit)
                            RoundIconButton(
                                Icons.Filled.Delete, "Delete recipe",
                                { showDeleteDialog = true },
                                tint = MaterialTheme.colorScheme.error,
                            )
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
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

@Composable
private fun DetailTabs(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            listOf("Ingredients", "Steps").forEachIndexed { index, label ->
                val isSelected = index == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) }),
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientList(lines: List<RecipeIngredientLine>) {
    Column {
        lines.forEachIndexed { index, line ->
            Column(Modifier.padding(vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        line.ingredientName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${line.quantity.formatQuantity()} ${line.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                line.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (index < lines.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun StepList(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        steps.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun Difficulty.label() = when (this) {
    Difficulty.EASY -> "Easy"
    Difficulty.MEDIUM -> "Medium"
    Difficulty.HARD -> "Hard"
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
