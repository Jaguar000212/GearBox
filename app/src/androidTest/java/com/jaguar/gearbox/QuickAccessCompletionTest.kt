package com.jaguar.gearbox

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.ToolSwitcher
import com.jaguar.gearbox.ui.screens.FavoritesScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class QuickAccessCompletionTest {
    @get:Rule val compose = createComposeRule()
    private val context = IsolatedToolTestContext()
    @After fun cleanup() { context.cleanup() }

    @Test fun quickSwitchFindsCategoryLabelsAndOpensOnlyAfterSelection() {
        val tool = Tools.byRoute(Tools.ROUTE_HASH_GENERATOR)!!
        var opened: Tool? = null
        compose.setContent { context.Provide { MaterialTheme { ToolSwitcher({ opened = it }, {}) } } }
        compose.onNodeWithText("Find a tool").performTextInput(tool.category.label)
        assertNull(opened)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(tool.name))
        compose.onNodeWithText(tool.name).performClick()
        assertEquals(tool.route, opened?.route)
    }

    @Test fun quickSwitchRecentSectionHonorsVisibilitySetting() {
        val recent = RecentToolsStore(context)
        recent.recordOpened(Tools.ROUTE_HASH_GENERATOR)
        compose.setContent { context.Provide { MaterialTheme { ToolSwitcher({}, {}) } } }
        compose.onNodeWithText("Recent tools").assertExists()
        compose.onAllNodesWithText(Tools.byRoute(Tools.ROUTE_HASH_GENERATOR)!!.name).assertCountEquals(1)
        compose.runOnIdle { recent.setShowRecentTools(false) }
        compose.onNodeWithText("Recent tools").assertDoesNotExist()
        assertEquals(listOf(Tools.ROUTE_HASH_GENERATOR), recent.recent)
    }

    @Test fun favoritesLaunchesExplicitPresetWithoutChangingFavoritesOrSavedData() {
        val store = ToolMemoryStore(context)
        val inputs = mapOf("algorithm" to "SHA256", "text" to "example")
        assertTrue(store.savePreset("My hash setup", Tools.ROUTE_HASH_GENERATOR, inputs))
        val favorites = FavoritesStore(context.getSharedPreferences("favorites", Context.MODE_PRIVATE))
        favorites.addAll(listOf(Tools.ROUTE_PERCENTAGE, Tools.ROUTE_BMI))
        val before = favorites.favorites.toList()
        var opened: Pair<String, Map<String, String>>? = null
        compose.setContent { context.Provide { MaterialTheme {
            FavoritesScreen(Tools.all, favorites, { error("Must use explicit preset callback") },
                onOpenPreset = { route, values -> opened = route to values })
        } } }
        compose.onNodeWithText("Saved presets").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("My hash setup").fetchSemanticsNodes().isNotEmpty() }
        assertNull(opened)
        compose.onNodeWithText("My hash setup").performClick()
        assertEquals(Tools.ROUTE_HASH_GENERATOR to inputs, opened)
        assertEquals(before, favorites.favorites)
        assertEquals(1, store.presets().size)
        assertTrue(store.history(Tools.ROUTE_HASH_GENERATOR).isEmpty())
    }

    @Test fun emptyPresetsExplainCreationWithoutOpeningTool() {
        val favorites = FavoritesStore(context.getSharedPreferences("favorites", Context.MODE_PRIVATE))
        compose.setContent { context.Provide { MaterialTheme {
            FavoritesScreen(Tools.all, favorites, {}, onOpenPreset = { _, _ -> error("No preset to launch") })
        } } }
        compose.onNodeWithText("Saved presets").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("No saved presets yet.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Close presets").performClick()
        compose.onNodeWithText("Your toolbox").assertExists()
    }
}
