package com.yeyint.recipeapp.feature.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.yeyint.recipeapp.ui.components.RecipeListRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.ui.components.EmptyView
import com.yeyint.recipeapp.ui.components.ErrorView
import com.yeyint.recipeapp.ui.components.LoadingView
import com.yeyint.recipeapp.ui.components.RecipeCard
import com.yeyint.recipeapp.ui.components.SectionHeader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onRecipeClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onCreateClick: () -> Unit,
    viewModel: HomeContract = koinViewModel<HomeViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var tabIndex by rememberSaveable { mutableStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    Scaffold(
        // MainScaffold already applies the window insets and passes them
        // to this screen; re-applying them here would double the top
        // padding (very visible on iOS, where the safe area is ~59pt).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (val state = uiState) {
            HomeUiState.Loading -> LoadingView(Modifier.padding(padding))
            is HomeUiState.Error -> ErrorView(state.error, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            is HomeUiState.Data -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    state.userName.trim().take(1).uppercase().ifEmpty { "?" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Hello",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    state.userName.ifBlank { "Chef" },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            IconButton(
                                onClick = onCreateClick,
                                modifier = Modifier.size(44.dp).background(
                                    MaterialTheme.colorScheme.surfaceVariant, CircleShape,
                                ),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Create recipe")
                            }
                        }
                        Text("Find your next meal", style = MaterialTheme.typography.headlineLarge)
                        Surface(
                            onClick = onSearchClick,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Search, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Search recipes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item {
                    HomeTabs(selected = tabIndex, onSelect = { tabIndex = it })
                }

                val recipes = state.recipesFor(HomeTab.entries[tabIndex])
                if (recipes.isEmpty()) {
                    item { EmptyView("No recipes yet", "Be the first to share one!") }
                } else {
                    val featured = recipes.first()
                    item(key = "featured-${featured.id}") {
                        RecipeCard(featured, onClick = { onRecipeClick(featured.id) })
                    }
                    val rest = recipes.drop(1)
                    if (rest.isNotEmpty()) {
                        item { SectionHeader("More to cook") }
                        items(rest, key = { it.id }) { recipe ->
                            RecipeListRow(recipe, onClick = { onRecipeClick(recipe.id) })
                        }
                    }
                }
            }
        }
    }
}

private enum class HomeTab(val label: String) {
    ForYou("For you"),
    Popular("Popular"),
    Quick("Quick"),
    New("New"),
}

private const val QUICK_MAX_MINUTES = 30

private fun HomeUiState.Data.recipesFor(tab: HomeTab): List<RecipeSummary> = when (tab) {
    HomeTab.ForYou -> recommended.ifEmpty { newest }
    HomeTab.Popular -> popular.ifEmpty { newest }
    HomeTab.Quick -> (recommended + popular + newest)
        .distinctBy { it.id }
        .filter { it.cookingTimeMinutes <= QUICK_MAX_MINUTES }
        .sortedBy { it.cookingTimeMinutes }
    HomeTab.New -> newest
}

@Composable
private fun HomeTabs(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            HomeTab.entries.forEachIndexed { index, tab ->
                val isSelected = index == selected
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .heightIn(min = 44.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        tab.label,
                        style = if (isSelected) MaterialTheme.typography.labelLarge
                        else MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                    )
                    Box(
                        Modifier
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onSurface
                                else Color.Transparent,
                            ),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

private class FakeHomeContract(state: HomeUiState) : HomeContract {
    override val uiState: StateFlow<HomeUiState> = MutableStateFlow(state)
    override fun refresh() = Unit
    override fun logoutName() = ""
}

private fun fakeRecipe(id: String, title: String, difficulty: Difficulty = Difficulty.MEDIUM) =
    RecipeSummary(
        id = id,
        title = title,
        description = "A delicious recipe you will love.",
        cookingTimeMinutes = 30,
        servings = 2,
        difficulty = difficulty,
        coverImageUrl = null,
        authorName = "Chef Preview",
        viewCount = 512,
        createdAt = "2025-01-01",
    )

private val previewRecipes = listOf(
    fakeRecipe("1", "Spaghetti Carbonara"),
    fakeRecipe("2", "Avocado Toast", Difficulty.EASY),
    fakeRecipe("3", "Beef Wellington", Difficulty.HARD),
)

@Preview
@Composable
private fun HomeScreenLoadingPreview() {
    MaterialTheme {
        Surface {
            HomeScreen(
                onRecipeClick = {},
                onSearchClick = {},
                onCreateClick = {},
                viewModel = FakeHomeContract(HomeUiState.Loading),
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenDataPreview() {
    MaterialTheme {
        Surface {
            HomeScreen(
                onRecipeClick = {},
                onSearchClick = {},
                onCreateClick = {},
                viewModel = FakeHomeContract(
                    HomeUiState.Data(
                        userName = "Ye Yint",
                        recommended = previewRecipes,
                        popular = previewRecipes.reversed(),
                        newest = previewRecipes,
                    ),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenEmptyPreview() {
    MaterialTheme {
        Surface {
            HomeScreen(
                onRecipeClick = {},
                onSearchClick = {},
                onCreateClick = {},
                viewModel = FakeHomeContract(
                    HomeUiState.Data(
                        userName = "",
                        recommended = emptyList(),
                        popular = emptyList(),
                        newest = emptyList(),
                    ),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenErrorPreview() {
    MaterialTheme {
        Surface {
            HomeScreen(
                onRecipeClick = {},
                onSearchClick = {},
                onCreateClick = {},
                viewModel = FakeHomeContract(HomeUiState.Error(AppError.Network)),
            )
        }
    }
}
