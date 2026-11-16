package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.screens.WidgetDiscoveryPreference
import org.junit.After
import org.junit.Rule
import org.junit.Test

class WidgetDiscoveryTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    @Test fun guideExplainsBothWidgetsAndPreviewIsolationWithoutLaunchingAnything() {
        compose.setContent { isolated.Provide { MaterialTheme { WidgetDiscoveryPreference() } } }
        compose.onNodeWithText("Home-screen widgets").performClick()
        compose.onNodeWithText("Counter: view and change", substring = true).assertExists()
        compose.onNodeWithText("snapshot, not a continuously ticking display", substring = true).assertExists()
        compose.onNodeWithText("long-press an empty area", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("separate GearBox Preview widget group", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Got it").performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.onNodeWithText("Home-screen widgets").assertExists()
    }
}
