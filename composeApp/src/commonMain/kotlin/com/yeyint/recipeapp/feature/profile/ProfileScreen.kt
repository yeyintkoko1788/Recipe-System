package com.yeyint.recipeapp.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yeyint.recipeapp.shared.domain.model.Difficulty
import com.yeyint.recipeapp.shared.domain.model.RecipeSummary
import com.yeyint.recipeapp.shared.domain.model.User
import com.yeyint.recipeapp.shared.domain.model.UserRole
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
fun ProfileScreen(
    onMyRecipeClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ProfileContract = koinViewModel<ProfileViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            uiState.user?.name?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(uiState.user?.name ?: "", style = MaterialTheme.typography.titleLarge)
                    Text(
                        uiState.user?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (uiState.user?.role == UserRole.ADMIN) {
                        Text("Admin", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(onClick = onSettingsClick) { Icon(Icons.Filled.Settings, "Settings") }
                IconButton(onClick = viewModel::logout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "Log out", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        item { SectionHeader("My recipes") }
        when {
            uiState.isLoadingRecipes -> item { LoadingView(Modifier.padding(24.dp)) }
            uiState.recipesError != null -> item {
                ErrorView(uiState.recipesError!!, onRetry = viewModel::refresh)
            }
            uiState.myRecipes.isEmpty() -> item {
                EmptyView("No recipes yet", "Recipes you create will show up here.")
            }
            else -> items(uiState.myRecipes, key = { it.id }) { recipe ->
                RecipeCard(recipe, onClick = { onMyRecipeClick(recipe.id) })
            }
        }
    }
}

private class FakeProfileContract(state: ProfileUiState) : ProfileContract {
    override val uiState: StateFlow<ProfileUiState> = MutableStateFlow(state)
    override fun refresh() = Unit
    override fun logout() = Unit
}

private val previewUser = User("u1", "Ye Yint", "ye@example.com", UserRole.USER)
private val previewAdminUser = User("u2", "Admin User", "admin@example.com", UserRole.ADMIN)

private fun fakeRecipe(id: String, title: String) = RecipeSummary(
    id = id, title = title,
    description = "A delicious recipe.",
    cookingTimeMinutes = 20, servings = 2, difficulty = Difficulty.EASY,
    coverImageUrl = null, authorName = "Ye Yint", viewCount = 100, createdAt = "2025-01-01",
)

@Preview
@Composable
private fun ProfileScreenLoadingPreview() {
    MaterialTheme {
        Surface {
            ProfileScreen(
                onMyRecipeClick = {}, onSettingsClick = {},
                viewModel = FakeProfileContract(ProfileUiState(user = previewUser, isLoadingRecipes = true)),
            )
        }
    }
}

@Preview
@Composable
private fun ProfileScreenDataPreview() {
    MaterialTheme {
        Surface {
            ProfileScreen(
                onMyRecipeClick = {}, onSettingsClick = {},
                viewModel = FakeProfileContract(
                    ProfileUiState(
                        user = previewUser,
                        isLoadingRecipes = false,
                        myRecipes = listOf(
                            fakeRecipe("1", "Spaghetti Carbonara"),
                            fakeRecipe("2", "Avocado Toast"),
                        ),
                    ),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ProfileScreenAdminPreview() {
    MaterialTheme {
        Surface {
            ProfileScreen(
                onMyRecipeClick = {}, onSettingsClick = {},
                viewModel = FakeProfileContract(
                    ProfileUiState(user = previewAdminUser, isLoadingRecipes = false),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ProfileScreenErrorPreview() {
    MaterialTheme {
        Surface {
            ProfileScreen(
                onMyRecipeClick = {}, onSettingsClick = {},
                viewModel = FakeProfileContract(
                    ProfileUiState(user = previewUser, isLoadingRecipes = false, recipesError = AppError.Network),
                ),
            )
        }
    }
}
