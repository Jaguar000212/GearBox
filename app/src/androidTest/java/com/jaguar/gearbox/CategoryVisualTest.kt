package com.jaguar.gearbox

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.ToolCategory
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.data.localizedLabel
import com.jaguar.gearbox.ui.components.CategoryHeading
import com.jaguar.gearbox.ui.components.categoryIcon
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryVisualTest {
    @get:Rule val compose = createComposeRule()
    private val context = IsolatedToolTestContext()
    @After fun cleanup() { context.cleanup() }

    private fun assertHeadings(dark: Boolean) {
        compose.setContent { context.Provide {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Column { ToolCategory.entries.forEach { category ->
                    CategoryHeading(category, Tools.all.count { it.category == category })
                } }
            }
        } }
        ToolCategory.entries.forEach { category ->
            val count = Tools.all.count { it.category == category }
            compose.onNode(hasText(category.localizedLabel(context)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                .assertExists().assert(hasText(context.resources.getQuantityString(R.plurals.screen_tool_count, count, count)))
        }
        // Icons carry no separate announcement; text and counts remain the meaning.
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)).assertCountEquals(0)
        assertEquals(7, ToolCategory.entries.map { categoryIcon(it).name }.distinct().size)
    }

    @Test fun allSevenHaveNamedCountedHeadingsInLightTheme() = assertHeadings(false)
    @Test fun allSevenHaveNamedCountedHeadingsInDarkTheme() = assertHeadings(true)
}
