package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.screens.FavoritesScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesReorderTest {
    @get:Rule val rule = createComposeRule()

    @Test fun visibleReorderButtonsMoveToolsAndDisableAtBoundaries() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("gearbox_prefs", 0)
        // Construct the store first so legacy unordered favorites are migrated before saving.
        val original = FavoritesStore(context).favorites.toList()
        val tools = Tools.all.take(2)
        prefs.edit().putString("favorite_routes", tools.joinToString(",") { it.route }).commit()
        try {
            val store = FavoritesStore(context)
            rule.setContent { MaterialTheme { FavoritesScreen(tools, store) {} } }
            rule.onNodeWithContentDescription("Move ${tools[0].name} up").assertIsNotEnabled()
            rule.onNodeWithContentDescription("Move ${tools[0].name} down").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(listOf(tools[1].route, tools[0].route), store.favorites.toList()) }
            rule.onNodeWithContentDescription("Move ${tools[0].name} down").assertIsNotEnabled()
        } finally {
            prefs.edit().putString("favorite_routes", original.joinToString(",")).commit()
            FavoritesStore(context) // Restore launcher shortcuts to the user's original order.
        }
    }
}
