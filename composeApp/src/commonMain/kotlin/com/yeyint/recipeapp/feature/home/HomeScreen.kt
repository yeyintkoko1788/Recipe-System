package com.yeyint.recipeapp.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Filled.Add, contentDescription = "Create recipe")
            }
        },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hello${if (state.userName.isBlank()) "" else ", ${state.userName}"} 🧑‍🍳",
                                style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "What are we cooking today?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Filled.Search, contentDescription = "Search recipes")
                        }
                    }
                }

                if (state.recommended.isNotEmpty()) {
                    item { SectionHeader("Recommended for you") }
                    item { HorizontalRecipeRow(state.recommended, onRecipeClick) }
                }
                if (state.popular.isNotEmpty()) {
                    item { SectionHeader("Popular") }
                    item { HorizontalRecipeRow(state.popular, onRecipeClick) }
                }
                item { SectionHeader("New recipes") }
                if (state.newest.isEmpty()) {
                    item { EmptyView("No recipes yet", "Be the first to share one!") }
                } else {
                    items(state.newest, key = { it.id }) { recipe ->
                        RecipeCard(recipe, onClick = { onRecipeClick(recipe.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalRecipeRow(recipes: List<RecipeSummary>, onRecipeClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(recipes, key = { it.id }) { recipe ->
            RecipeCard(
                recipe,
                onClick = { onRecipeClick(recipe.id) },
                modifier = Modifier.width(280.dp),
            )
        }
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
