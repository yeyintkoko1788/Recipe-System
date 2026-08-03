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
import com.yeyint.recipeapp.ui.components.EmptyView
import com.yeyint.recipeapp.ui.components.ErrorView
import com.yeyint.recipeapp.ui.components.LoadingView
import com.yeyint.recipeapp.ui.components.RecipeCard
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
