package com.yeyint.recipeapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yeyint.recipeapp.theme.AppColors
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import com.yeyint.recipeapp.feature.explore.ExploreScreen
import com.yeyint.recipeapp.feature.home.HomeScreen
import com.yeyint.recipeapp.feature.ingredients.IngredientListScreen
import com.yeyint.recipeapp.feature.ingredients.IngredientListViewModel
import com.yeyint.recipeapp.shared.domain.repository.PantryRepository
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.yeyint.recipeapp.feature.pantry.PantryScreen
import com.yeyint.recipeapp.feature.profile.ProfileScreen
import com.yeyint.recipeapp.feature.recipedetail.RecipeDetailScreen
import com.yeyint.recipeapp.feature.recipeeditor.RecipeEditorScreen
import com.yeyint.recipeapp.feature.search.SearchScreen
import com.yeyint.recipeapp.feature.settings.SettingsScreen
import com.yeyint.recipeapp.feature.shopping.ShoppingListScreen
import androidx.navigation.toRoute

private data class BottomTab(val route: Route, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Route.Home, "Home", Icons.Filled.Home),
    BottomTab(Route.Explore, "Explore", Icons.Filled.Explore),
    BottomTab(Route.Pantry, "Pantry", Icons.Filled.Kitchen),
    BottomTab(Route.Shopping, "Shopping", Icons.Filled.ShoppingCart),
    BottomTab(Route.Profile, "Profile", Icons.Filled.Person),
)

/** Floating dark pill; the selected tab is a green circle. */
@Composable
private fun FloatingNavBar(
    selectedRoute: (BottomTab) -> Boolean,
    onSelect: (BottomTab) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = CircleShape,
            color = AppColors.NavPill,
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomTabs.forEach { tab ->
                    val selected = selectedRoute(tab)
                    IconButton(
                        onClick = { onSelect(tab) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                CircleShape,
                            ),
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) Color.White else AppColors.NavPillIcon,
                        )
                    }
                }
            }
        }
    }
}

/** Main app shell: bottom navigation + nested NavHost. */
@Composable
fun MainScaffold(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val onBottomTab = bottomTabs.any { tab -> currentDestination?.hasRoute(tab.route::class) == true }

    Scaffold(
        bottomBar = {
            if (onBottomTab) {
                FloatingNavBar(
                    selectedRoute = { tab -> currentDestination?.hasRoute(tab.route::class) == true },
                    onSelect = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier.padding(padding),
        ) {
            composable<Route.Home> {
                HomeScreen(
                    onRecipeClick = { navController.navigate(Route.RecipeDetail(it)) },
                    onSearchClick = { navController.navigate(Route.Search) },
                    onCreateClick = { navController.navigate(Route.RecipeEditor()) },
                )
            }
            composable<Route.Explore> {
                ExploreScreen(onRecipeClick = { navController.navigate(Route.RecipeDetail(it)) })
            }
            composable<Route.Pantry> {
                PantryScreen(onBrowseIngredients = { navController.navigate(Route.Ingredients) })
            }
            composable<Route.Shopping> { ShoppingListScreen() }
            composable<Route.Profile> {
                ProfileScreen(
                    onMyRecipeClick = { navController.navigate(Route.RecipeDetail(it)) },
                    onSettingsClick = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.Search> {
                SearchScreen(
                    onRecipeClick = { navController.navigate(Route.RecipeDetail(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.Ingredients> {
                val vm = koinViewModel<IngredientListViewModel>()
                vm.pantryRepository = koinInject<PantryRepository>()
                IngredientListScreen(onBack = { navController.popBackStack() }, viewModel = vm)
            }
            composable<Route.Settings> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<Route.RecipeDetail> { entry ->
                val route = entry.toRoute<Route.RecipeDetail>()
                RecipeDetailScreen(
                    recipeId = route.recipeId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Route.RecipeEditor(route.recipeId)) },
                )
            }
            composable<Route.RecipeEditor> { entry ->
                val route = entry.toRoute<Route.RecipeEditor>()
                RecipeEditorScreen(
                    recipeId = route.recipeId,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
