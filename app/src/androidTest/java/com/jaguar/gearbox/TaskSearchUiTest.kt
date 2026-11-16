package com.jaguar.gearbox

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.ToolSwitcher
import com.jaguar.gearbox.ui.screens.HomeScreen
import com.jaguar.gearbox.ui.theme.ThemeMode
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TaskSearchUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = IsolatedToolTestContext()
    private val favorites = FavoritesStore(context.getSharedPreferences("favorites", Context.MODE_PRIVATE))
    @After fun cleanup() { context.cleanup() }

    @Test fun homeOperationSearchRespectsCategoryAndDispatchesOnlyOnTap() {
        var openedTool: Tool? = null
        var selected: ToolSearchOperation? = null
        compose.setContent { context.Provide { MaterialTheme {
            HomeScreen(Tools.all, favorites, RecentToolsStore(context), onOpenTool = { openedTool = it },
                onOpenOperation = { tool, operation -> openedTool = tool; selected = operation })
        } } }
        compose.onNode(hasText("All tools") and hasClickAction()).performClick()
        compose.onNode(hasText(ToolCategory.MONEY.localizedLabel(context)) and isSelectable()).performClick()
        compose.onNodeWithText("Search tools").performTextInput("rectangle area")
        compose.onNodeWithText(context.getString(R.string.layout_no_tools_found)).assertExists()
        compose.runOnIdle { assertNull(openedTool); assertNull(selected) }
        compose.onNodeWithText("Search all tools").performScrollTo().performClick()
        val operation = ToolSearchCatalog.resolveOperation(context, "geometry.rectangle")!!.operation!!
        compose.onNode(hasText(operation.label) and hasClickAction()).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(Tools.ROUTE_GEOMETRY, openedTool?.route)
            assertEquals(mapOf("shape" to "RECTANGLE"), selected?.modeInputs)
        }
    }

    @Test fun switcherTaskSearchDispatchesModeNotPlainOpen() {
        var selected: ToolSearchOperation? = null
        compose.setContent { context.Provide { MaterialTheme {
            ToolSwitcher({ error("An operation must use its explicit callback") }, {},
                onOpenOperation = { _, operation -> selected = operation })
        } } }
        compose.onNodeWithText("Find a tool").performTextInput("sha512")
        val operation = ToolSearchCatalog.resolveOperation(context, "hash.sha512")!!.operation!!
        compose.runOnIdle { assertNull(selected) }
        compose.onNode(hasText(operation.label) and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(mapOf("algorithm" to "SHA512"), selected?.modeInputs) }
    }

    @Test fun sameToolModeConfirmationCanCancelAndPreservesExistingInput() {
        compose.setContent { context.Provide {
            var pendingRoute by remember { mutableStateOf<String?>(Tools.ROUTE_GEOMETRY) }
            GearBoxApp(ThemeMode.SYSTEM, {}, false, {}, false, {}, pendingRoute = pendingRoute,
                onPendingRouteConsumed = { pendingRoute = null }, favoritesStore = favorites)
        } }
        compose.onNodeWithText("Radius").performScrollTo().performTextReplacement("7")
        requestRectangle()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Radius").assertTextContains("7")
        requestRectangle()
        compose.onNodeWithText("Select mode").performClick()
        compose.onNodeWithText("Width").assertTextContains("7")
        compose.onNodeWithText("Height").assertExists()
        compose.onNodeWithText("Radius").assertDoesNotExist()
    }

    private fun requestRectangle() {
        compose.onNodeWithContentDescription("More tool actions").performClick()
        compose.onNodeWithText("Switch tool").performClick()
        compose.onNodeWithText("Find a tool").performTextInput("rectangle area")
        val operation = ToolSearchCatalog.resolveOperation(context, "geometry.rectangle")!!.operation!!
        compose.onNode(hasText(operation.label) and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Select ${operation.label}?").assertExists()
    }
}
