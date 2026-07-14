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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.RecentToolsStore
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tool
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.screens.FavoritesScreen
import com.jaguar.gearbox.ui.screens.HomeScreen
import com.jaguar.gearbox.ui.screens.SettingsScreen
import com.jaguar.gearbox.ui.theme.AppTheme
import com.jaguar.gearbox.ui.theme.LocalHapticsEnabled
import com.jaguar.gearbox.ui.theme.ThemeMode
import com.jaguar.gearbox.ui.theme.resolveDarkTheme

private const val KEY_THEME_MODE = "settings.theme_mode"
private const val KEY_DYNAMIC_COLOR = "settings.dynamic_color"
private const val KEY_HAPTICS = "settings.haptics"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { SimplePrefsStore(context) }
            var themeMode by remember {
                mutableStateOf(
                    ThemeMode.entries.firstOrNull {
                        it.name == prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
                    } ?: ThemeMode.SYSTEM,
                )
            }
            var dynamicColorEnabled by remember {
                mutableStateOf(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
            }
            var hapticsEnabled by remember {
                mutableStateOf(prefs.getBoolean(KEY_HAPTICS, true))
            }

            CompositionLocalProvider(LocalHapticsEnabled provides hapticsEnabled) {
                AppTheme(
                    darkTheme = themeMode.resolveDarkTheme(),
                    dynamicColor = dynamicColorEnabled,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        GearBoxApp(
                            themeMode = themeMode,
                            onThemeModeChange = {
                                themeMode = it
                                prefs.putString(KEY_THEME_MODE, it.name)
                            },
                            dynamicColorEnabled = dynamicColorEnabled,
                            onDynamicColorChange = {
                                dynamicColorEnabled = it
                                prefs.putBoolean(KEY_DYNAMIC_COLOR, it)
                            },
                            hapticsEnabled = hapticsEnabled,
                            onHapticsChange = {
                                hapticsEnabled = it
                                prefs.putBoolean(KEY_HAPTICS, it)
                            },
                        )
                    }
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
fun GearBoxApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val favorites = remember { FavoritesStore(context) }
    val recentTools = remember { RecentToolsStore(context) }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isTopLevel = currentRoute in tabs.map { it.route }

    val openTool: (Tool) -> Unit = { tool ->
        recentTools.recordOpened(tool.route)
        navController.navigate(tool.route)
    }

    Scaffold(
        topBar = {
            if (isTopLevel) {
                TopAppBar(title = { Text(stringForRoute(currentRoute)) })
            }
        },
        bottomBar = {
            if (isTopLevel) BottomNavigationBar(navController, currentRoute)
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = ROUTE_HOME) {
                composable(ROUTE_HOME) {
                    HomeScreen(
                        tools = Tools.all,
                        favorites = favorites,
                        recentTools = recentTools,
                        onOpenTool = openTool,
                    )
                }
                composable(ROUTE_FAVORITES) {
                    FavoritesScreen(
                        tools = Tools.all,
                        favorites = favorites,
                        onOpenTool = openTool,
                    )
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        dynamicColorEnabled = dynamicColorEnabled,
                        onDynamicColorChange = onDynamicColorChange,
                        hapticsEnabled = hapticsEnabled,
                        onHapticsChange = onHapticsChange,
                    )
                }

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
