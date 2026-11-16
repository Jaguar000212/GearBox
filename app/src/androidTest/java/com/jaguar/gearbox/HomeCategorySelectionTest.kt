package com.jaguar.gearbox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.screens.HomeScreen
import org.junit.Rule
import org.junit.Test

class HomeCategorySelectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchAllToolsKeepsQueryAndRemovesOnlyCategoryScope() {
        // Use test-package preferences, never the installed app's saved favorites.
        val context = InstrumentationRegistry.getInstrumentation().context
        val tools = listOf(
            Tool("Tip", "Split a bill", Icons.Default.Star, "test-tip", ToolCategory.MONEY, {}),
            Tool("Counter", "Count objects", Icons.Default.Star, "test-counter", ToolCategory.TIME, {}),
        )
        compose.setContent {
            MaterialTheme {
                HomeScreen(tools, FavoritesStore(context), RecentToolsStore(context), {})
            }
        }
        compose.onNodeWithText("All tools").performClick()
        compose.onNodeWithText("Money").performClick()
        compose.onNodeWithText("Search tools").performTextInput("Counter")
        compose.onNodeWithText("Search all tools").performScrollTo().performClick()
        compose.onNode(hasText("Counter") and hasClickAction()).assertExists()
        compose.onNode(hasText("Counter") and hasSetTextAction()).assertExists()
    }
}
