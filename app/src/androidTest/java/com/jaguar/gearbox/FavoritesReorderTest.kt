package com.jaguar.gearbox

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.screens.FavoritesScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class FavoritesReorderTest {
    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val name = "favorites_reorder_${UUID.randomUUID()}"
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    @After fun cleanup() { context.deleteSharedPreferences(name) }

    @Test fun visibleReorderButtonsMoveToolsAndDisableAtBoundaries() {
        val tools = Tools.all.take(2)
        // Inject isolated preferences and a no-op shortcut callback: never change the launcher.
        val store = FavoritesStore(prefs)
        store.addAll(tools.map { it.route })
        rule.setContent { MaterialTheme { FavoritesScreen(tools, store) {} } }
        rule.onNodeWithContentDescription("Move ${tools[0].name} up").assertDoesNotExist()
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithContentDescription("Move ${tools[0].name} up").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Move ${tools[0].name} down").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf(tools[1].route, tools[0].route), store.favorites.toList()) }
        rule.onNodeWithContentDescription("Move ${tools[0].name} down").assertIsNotEnabled()
    }
}
