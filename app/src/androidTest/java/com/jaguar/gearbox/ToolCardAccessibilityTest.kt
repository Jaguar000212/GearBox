package com.jaguar.gearbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tool
import com.jaguar.gearbox.data.ToolCategory
import com.jaguar.gearbox.ui.components.ToolCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ToolCardAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun favoriteHasNamedTouchTargetAndLargeTextRemainsVisible() {
        var toggled = false
        val tool = Tool(
            name = "Example tool",
            description = "A description that can wrap rather than being clipped by a fixed-height card.",
            icon = Icons.Filled.Star,
            route = "test/example",
            category = ToolCategory.UTILITIES,
            content = {},
        )
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        ToolCard(tool, false, {}, { toggled = true })
                    }
                }
            }
        }
        rule.onNodeWithText(tool.description).assertIsDisplayed()
        rule.onNodeWithContentDescription("Add Example tool to favourites")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.runOnIdle { assertTrue(toggled) }
    }
}
