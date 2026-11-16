package com.jaguar.gearbox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.RecentToolsStore
import com.jaguar.gearbox.data.Tool
import com.jaguar.gearbox.data.ToolCategory
import com.jaguar.gearbox.ui.screens.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeDiscoveryTest {
    @get:Rule val rule = createComposeRule()

    @Test fun emptySearchCanResetAndOpenTheTool() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tool = Tool("Example calculator", "A readable description", Icons.Filled.Star,
            "test/home-example", ToolCategory.CALCULATORS, {})
        var opened: String? = null
        val favorites = FavoritesStore(context)
        val recents = RecentToolsStore(context)
        rule.setContent {
            MaterialTheme {
                HomeScreen(listOf(tool), favorites, recents, onOpenTool = { opened = it.route })
            }
        }
        rule.onNodeWithText("Search tools").performTextInput("no matching tool")
        rule.onNodeWithText("Clear filters").performScrollTo().performClick()
        rule.onNodeWithText("1 tool").assertIsDisplayed()
        rule.onNodeWithText(tool.name).performScrollTo().performClick()
        rule.runOnIdle { assertEquals(tool.route, opened) }
    }
}
