package com.yeyint.recipeapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.yeyint.recipeapp.feature.pantry.PantryScreen
import com.yeyint.recipeapp.feature.profile.ProfileScreen
import com.yeyint.recipeapp.feature.recipedetail.RecipeDetailScreen
import com.yeyint.recipeapp.feature.recipeeditor.RecipeEditorScreen
import com.yeyint.recipeapp.feature.search.SearchScreen
import com.yeyint.recipeapp.feature.settings.SettingsScreen
import com.yeyint.recipeapp.feature.shopping.ShoppingListScreen
import androidx.navigation.toRoute

private data class BottomTab(val route: Route, val label: String, val icon: @Composable () -> Unit)

private val bottomTabs = listOf(
    BottomTab(Route.Home, "Home") { Icon(Icons.Filled.Home, null) },
    BottomTab(Route.Explore, "Explore") { Icon(Icons.Filled.Explore, null) },
    BottomTab(Route.Pantry, "Pantry") { Icon(Icons.Filled.Kitchen, null) },
    BottomTab(Route.Shopping, "Shopping") { Icon(Icons.Filled.ShoppingCart, null) },
    BottomTab(Route.Profile, "Profile") { Icon(Icons.Filled.Person, null) },
)

/** Main app shell: bottom navigation + nested NavHost. */
@Composable
fun MainScaffold(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val onBottomTab = bottomTabs.any { tab -> currentDestination?.hasRoute(tab.route::class) == true }

    Scaffold(
        bottomBar = {
            if (onBottomTab) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination?.hasRoute(tab.route::class) == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = tab.icon,
                            label = { Text(tab.label) },
                        )
                    }
                }
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
                IngredientListScreen(onBack = { navController.popBackStack() })
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
