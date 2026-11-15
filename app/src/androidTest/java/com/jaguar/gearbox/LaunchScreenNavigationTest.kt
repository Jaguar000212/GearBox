package com.jaguar.gearbox

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.jaguar.gearbox.data.LaunchScreen
import com.jaguar.gearbox.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class LaunchScreenNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun favoritesStartsSelectedAndChangingPreferenceDoesNotNavigateAway() {
        compose.setContent {
            var screen by remember { mutableStateOf(LaunchScreen.FAVORITES) }
            GearBoxApp(ThemeMode.SYSTEM, {}, false, {}, false, {},
                launchScreen = screen, onLaunchScreenChange = { screen = it })
        }
        compose.onNode(hasText("Favorites") and isSelectable()).assertIsSelected()
        compose.onNode(hasText("Settings") and isSelectable()).performClick()
        compose.onNodeWithText("Launch screen").performClick()
        compose.onNode(hasText("Home") and isSelectable()).performClick()
        compose.onNode(hasText("Settings") and isSelectable()).assertIsSelected()
        compose.onNodeWithText("Home · On a fresh app launch").assertExists()
    }

    @Test fun shortcutStillOpensToolAndBackReturnsToPreferredTab() {
        compose.setContent {
            var route by remember { mutableStateOf<String?>(com.jaguar.gearbox.data.Tools.ROUTE_RANDOM) }
            GearBoxApp(ThemeMode.SYSTEM, {}, false, {}, false, {},
                launchScreen = LaunchScreen.FAVORITES, pendingRoute = route,
                onPendingRouteConsumed = { route = null })
        }
        compose.onNodeWithText("Generate", substring = true).assertExists()
        pressBack()
        compose.onNode(hasText("Favorites") and isSelectable()).assertIsSelected()
    }
}
