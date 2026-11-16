package com.jaguar.gearbox

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.screens.FavoritesScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class FavoritesChooserRestorationTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val prefsName = "favorites_chooser_${UUID.randomUUID()}"
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    @After fun cleanup() { context.deleteSharedPreferences(prefsName) }

    @Test fun chosenToolsSurviveRecreationWithoutApplyingUntilAdd() {
        val tools = Tools.all.take(2)
        val store = FavoritesStore(prefs)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { FavoritesScreen(tools, store, onOpenTool = {}) } }
        compose.onNodeWithText("Choose tools").performClick()
        compose.onNodeWithText(tools[0].name).performClick()
        compose.onNodeWithText(tools[1].name).performClick()
        compose.onNodeWithText("Add 2").assertIsEnabled()
        compose.runOnIdle { assertEquals(emptyList<String>(), store.favorites.toList()) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Add 2").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(tools.map { it.route }, store.favorites.toList()) }
        compose.onNodeWithText("Choose tools").assertIsFocused()
    }

    @Test fun cancelDoesNotApplyAndReturnsFocusToChooseTools() {
        val store = FavoritesStore(prefs)
        val tools = Tools.all.take(1)
        compose.setContent { MaterialTheme { FavoritesScreen(tools, store, onOpenTool = {}) } }
        compose.onNodeWithText("Choose tools").performClick()
        compose.onNodeWithText(tools[0].name).performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(emptyList<String>(), store.favorites.toList()) }
        compose.onNodeWithText("Choose tools").assertIsFocused()
    }
}
