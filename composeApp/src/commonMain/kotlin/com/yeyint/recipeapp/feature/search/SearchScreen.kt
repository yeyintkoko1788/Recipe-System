package com.yeyint.recipeapp.feature.search

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.ui.components.EmptyView
import com.yeyint.recipeapp.ui.components.ErrorView
import com.yeyint.recipeapp.ui.components.LoadingView
import com.yeyint.recipeapp.ui.components.RecipeCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onRecipeClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchContract = koinViewModel<SearchViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        // MainScaffold already applies the window insets and passes them
        // to this screen; re-applying them here would double the top
        // padding (very visible on iOS, where the safe area is ~59pt).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search recipes…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.difficulty == null,
                    onClick = { viewModel.setDifficulty(null) },
                    label = { Text("All") },
                )
                Difficulty.entries.forEach { difficulty ->
                    FilterChip(
                        selected = uiState.difficulty == difficulty,
                        onClick = { viewModel.setDifficulty(difficulty) },
                        label = { Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
                FilterChip(
                    selected = uiState.sortBy == "popularity",
                    onClick = {
                        viewModel.setSort(if (uiState.sortBy == "popularity") "createdAt" else "popularity")
                    },
                    label = { Text("Most viewed") },
                )
            }

            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(uiState.error!!)
                uiState.results.isEmpty() && uiState.hasSearched ->
                    EmptyView("No recipes found", "Try a different search or filter.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.results, key = { it.id }) { recipe ->
                        RecipeCard(recipe, onClick = { onRecipeClick(recipe.id) })
                    }
                    if (uiState.hasMore) {
                        item { LaunchedEffect(uiState.results.size) { viewModel.loadMore() } }
                    }
                }
            }
        }
    }
}

private class FakeSearchContract(state: SearchUiState = SearchUiState()) : SearchContract {
    override val uiState: StateFlow<SearchUiState> = MutableStateFlow(state)
    override fun onQueryChange(query: String) = Unit
    override fun setDifficulty(difficulty: Difficulty?) = Unit
    override fun setSort(sortBy: String) = Unit
    override fun loadMore() = Unit
}

private fun fakeRecipe(id: String, title: String) = RecipeSummary(
    id = id, title = title,
    description = "A delicious recipe you will love.",
    cookingTimeMinutes = 30, servings = 2, difficulty = Difficulty.EASY,
    coverImageUrl = null, authorName = "Chef Preview", viewCount = 200, createdAt = "2025-01-01",
)

@Preview
@Composable
private fun SearchScreenIdlePreview() {
    MaterialTheme {
        Surface {
            SearchScreen(onRecipeClick = {}, onBack = {}, viewModel = FakeSearchContract())
        }
    }
}

@Preview
@Composable
private fun SearchScreenLoadingPreview() {
    MaterialTheme {
        Surface {
            SearchScreen(
                onRecipeClick = {}, onBack = {},
                viewModel = FakeSearchContract(SearchUiState(query = "pasta", isLoading = true)),
            )
        }
    }
}

@Preview
@Composable
private fun SearchScreenResultsPreview() {
    MaterialTheme {
        Surface {
            SearchScreen(
                onRecipeClick = {}, onBack = {},
                viewModel = FakeSearchContract(
                    SearchUiState(
                        query = "pasta",
                        hasSearched = true,
                        results = listOf(
                            fakeRecipe("1", "Spaghetti Carbonara"),
                            fakeRecipe("2", "Penne Arrabbiata"),
                        ),
                    ),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun SearchScreenNoResultsPreview() {
    MaterialTheme {
        Surface {
            SearchScreen(
                onRecipeClick = {}, onBack = {},
                viewModel = FakeSearchContract(SearchUiState(query = "xyz", hasSearched = true)),
            )
        }
    }
}

@Preview
@Composable
private fun SearchScreenErrorPreview() {
    MaterialTheme {
        Surface {
            SearchScreen(
                onRecipeClick = {}, onBack = {},
                viewModel = FakeSearchContract(SearchUiState(error = AppError.Network)),
            )
        }
    }
}
