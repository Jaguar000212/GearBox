package com.jaguar.gearbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.screens.FavoritesScreen
import com.jaguar.gearbox.ui.screens.HomeScreen
import com.jaguar.gearbox.ui.screens.SettingsScreen
import com.jaguar.gearbox.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GearBoxApp()
                }
            }
        }
    }
}

private const val ROUTE_HOME = "home"
private const val ROUTE_FAVORITES = "favorites"
private const val ROUTE_SETTINGS = "settings"

private data class TabItem(val title: String, val route: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Home", ROUTE_HOME, Icons.Filled.Home),
    TabItem("Favorites", ROUTE_FAVORITES, Icons.Filled.Star),
    TabItem("Settings", ROUTE_SETTINGS, Icons.Filled.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearBoxApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val favorites = remember { FavoritesStore(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isTopLevel = currentRoute in tabs.map { it.route }

    Scaffold(
        topBar = {
            if (isTopLevel) {
                TopAppBar(title = { Text(stringForRoute(currentRoute)) })
            }
        },
        bottomBar = {
            if (isTopLevel) BottomNavigationBar(navController, currentRoute)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = ROUTE_HOME) {
                composable(ROUTE_HOME) {
                    HomeScreen(
                        tools = Tools.all,
                        favorites = favorites,
                        onOpenTool = { navController.navigate(it.route) },
                        onShowDescription = { tool ->
                            scope.launch { snackbarHostState.showSnackbar(tool.description) }
                        },
                    )
                }
                composable(ROUTE_FAVORITES) {
                    FavoritesScreen(
                        tools = Tools.all,
                        favorites = favorites,
                        onOpenTool = { navController.navigate(it.route) },
                        onShowDescription = { tool ->
                            scope.launch { snackbarHostState.showSnackbar(tool.description) }
                        },
                    )
                }
                composable(ROUTE_SETTINGS) { SettingsScreen() }

                // Every tool destination registers itself here - adding a new tool only means
                // adding one Tool(...) entry to Tools.all, not a new composable(...) line too.
                Tools.all.forEach { tool ->
                    composable(tool.route) { tool.content { navController.popBackStack() } }
                }
            }
        }
    }
}

private fun stringForRoute(route: String?): String = when (route) {
    ROUTE_FAVORITES -> "Favorites"
    ROUTE_SETTINGS -> "Settings"
    else -> "GearBox"
}

@Composable
private fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                label = { Text(tab.title) },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
            )
        }
    }
}
