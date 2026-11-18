package com.jaguar.gearbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.ToolHelpCatalog
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ToolHelpTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everyCatalogToolHasSpecificGuidanceAndUniqueFallbackIcon() {
        assertEquals(Tools.all.map { it.route }.toSet(), ToolHelpCatalog.all.keys)
        assertEquals(59, ToolHelpCatalog.all.size)
        assertEquals(Tools.all.size, Tools.all.map { it.icon }.distinct().size)
        ToolHelpCatalog.all.values.forEach {
            assertTrue(it.instructions.isNotBlank())
            assertTrue(it.example.isNotBlank())
            assertTrue(it.limitation.isNotBlank())
        }
        assertNull(ToolHelpCatalog.forRoute("unknown"))
    }

    @Test fun helpIsReadOnlyAndScrollableAtLargeFontSize() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        var input by remember { mutableStateOf("my existing input") }
                        ToolScaffold("Localized title", Tools.byRoute(Tools.ROUTE_AVERAGE)!!.icon, {}) {
                            OutlinedTextField(input, { input = it })
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("More tool actions").performClick()
        compose.onNodeWithText("Help & examples").performClick()
        compose.onNodeWithText("Help: Localized title").assertExists()
        compose.onNodeWithText(ToolHelpCatalog.forRoute(Tools.ROUTE_AVERAGE)!!.limitation).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("my existing input").assertExists()
        compose.onNodeWithText("Help: Localized title").assertDoesNotExist()
    }
}
