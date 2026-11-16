package com.jaguar.gearbox

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.RecentToolsStore
import com.jaguar.gearbox.ui.screens.SettingsScreen
import com.jaguar.gearbox.ui.theme.ThemeMode
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class RecentToolsSettingsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val directory = File(InstrumentationRegistry.getInstrumentation().context.cacheDir,
        "recent_ui_${UUID.randomUUID()}")
    private val store = RecentToolsStore.inFile(File(directory, "recent.json"))
    @After fun cleanup() { directory.deleteRecursively() }

    @Test fun togglesAreIndependentAndClearRequiresConfirmation() {
        store.recordOpened("tool/tip")
        compose.setContent {
            MaterialTheme { SettingsScreen(ThemeMode.SYSTEM, {}, false, {}, false, {}, recentTools = store) }
        }
        compose.onNodeWithText("Recent tools").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Show recent tools").performClick()
        compose.waitUntil(5000) { !store.showRecentTools }
        assertTrue(store.recordRecentTools)
        compose.onNodeWithText("Clear recent history").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("tool/tip"), store.recent)
        compose.onNodeWithText("Clear recent history").performScrollTo().performClick()
        compose.onNodeWithText("Clear", substring = false).performClick()
        compose.waitUntil(5000) { store.recent.isEmpty() }
        assertTrue(store.recordRecentTools)
        assertFalse(store.showRecentTools)
    }
}
