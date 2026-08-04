package com.yeyint.recipeapp.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExploreScreen(
    onRecipeClick: (String) -> Unit,
    viewModel: ExploreContract = koinViewModel<ExploreViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = ExploreTab.entries

    Column(Modifier.fillMaxSize()) {
        Text(
            "Explore",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        TabRow(selectedTabIndex = tabs.indexOf(uiState.tab)) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab == uiState.tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            when (tab) {
                                ExploreTab.POPULAR -> "Popular"
                                ExploreTab.NEW -> "New"
                                ExploreTab.RECOMMENDED -> "For you"
                            },
                        )
                    },
                )
            }
        }

        when {
            uiState.isLoading -> LoadingView()
            uiState.error != null && uiState.recipes.isEmpty() ->
                ErrorView(uiState.error!!, onRetry = viewModel::refresh)
            uiState.recipes.isEmpty() -> EmptyView("Nothing here yet", "Check back soon!")
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.recipes, key = { it.id }) { recipe ->
                    RecipeCard(recipe, onClick = { onRecipeClick(recipe.id) })
                }
                if (uiState.hasMore) {
                    item {
                        LaunchedEffect(uiState.recipes.size) { viewModel.loadMore() }
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

private class FakeExploreContract(state: ExploreUiState) : ExploreContract {
    override val uiState: StateFlow<ExploreUiState> = MutableStateFlow(state)
    override fun selectTab(tab: ExploreTab) = Unit
    override fun loadMore() = Unit
    override fun refresh() = Unit
}

private fun fakeRecipe(id: String, title: String) = RecipeSummary(
    id = id, title = title,
    description = "A delicious recipe you will love.",
    cookingTimeMinutes = 30, servings = 2, difficulty = Difficulty.MEDIUM,
    coverImageUrl = null, authorName = "Chef Preview", viewCount = 512, createdAt = "2025-01-01",
)

@Preview
@Composable
private fun ExploreScreenLoadingPreview() {
    MaterialTheme {
        Surface {
            ExploreScreen(
                onRecipeClick = {},
                viewModel = FakeExploreContract(ExploreUiState(isLoading = true)),
            )
        }
    }
}

@Preview
@Composable
private fun ExploreScreenDataPreview() {
    MaterialTheme {
        Surface {
            ExploreScreen(
                onRecipeClick = {},
                viewModel = FakeExploreContract(
                    ExploreUiState(
                        tab = ExploreTab.POPULAR,
                        isLoading = false,
                        recipes = listOf(
                            fakeRecipe("1", "Spaghetti Carbonara"),
                            fakeRecipe("2", "Avocado Toast"),
                            fakeRecipe("3", "Beef Wellington"),
                        ),
                    ),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ExploreScreenEmptyPreview() {
    MaterialTheme {
        Surface {
            ExploreScreen(
                onRecipeClick = {},
                viewModel = FakeExploreContract(ExploreUiState(isLoading = false)),
            )
        }
    }
}

@Preview
@Composable
private fun ExploreScreenErrorPreview() {
    MaterialTheme {
        Surface {
            ExploreScreen(
                onRecipeClick = {},
                viewModel = FakeExploreContract(
                    ExploreUiState(isLoading = false, error = AppError.Network),
                ),
            )
        }
    }
}
