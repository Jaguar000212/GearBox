package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.ToolAutomaticMemory
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.screens.ToolMemorySettingsSection
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PrivateToolMemorySettingsTest {
    @get:Rule val compose = createComposeRule()
    private val context = IsolatedToolTestContext()
    private val store = ToolMemoryStore(context)
    private val route = Tools.ROUTE_TEXT_TOOLS
    @After fun cleanup() { context.cleanup() }

    private fun openSettings() {
        compose.setContent { context.Provide { MaterialTheme { ToolMemorySettingsSection() } } }
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Tool memory & history") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Tool memory & history").performClick()
    }

    private fun seed() {
        assertTrue(store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true,
            automaticMemory = mapOf(route to ToolAutomaticMemory(true, true)))))
        assertTrue(store.record(route, mapOf("input" to "private example"), "SAVED HISTORY"))
        assertTrue(store.saveResult(route, mapOf("input" to "last example"), "SAVED RESULT"))
        assertTrue(store.savePreset("Manual preset", route, mapOf("input" to "manual")))
        assertTrue(store.saveToWorkbench("Manual result", route, emptyMap(), "manual output"))
    }

    @Test fun allSensitiveInputToolsStartWithSeparateAutomaticChoicesOff() {
        openSettings()
        listOf("Text Tools", "QR Generator", "Hash Generator").forEach { name ->
            compose.onNodeWithContentDescription("$name: automatic history").performScrollTo().assertIsOff()
            compose.onNodeWithContentDescription("$name: remember last result").performScrollTo().assertIsOff()
        }
        compose.onNodeWithContentDescription("Text Tools: automatic history").performScrollTo().performClick()
        compose.waitUntil(5_000) { store.settings().automaticMemoryFor(route).historyEnabled }
        assertFalse(store.settings().automaticMemoryFor(route).rememberResults)
        assertFalse(store.settings().permitsHistory(route)) // Global history is still off.
    }

    @Test fun disablingAutomaticSavingRetainsDataForExplicitReview() {
        seed()
        openSettings()
        compose.onNodeWithContentDescription("Text Tools: automatic history").performScrollTo().performClick()
        compose.waitUntil(5_000) { !store.settings().automaticMemoryFor(route).historyEnabled }
        compose.onNodeWithContentDescription("Text Tools: remember last result").performScrollTo().performClick()
        compose.waitUntil(5_000) { !store.settings().automaticMemoryFor(route).rememberResults }
        assertEquals(1, store.history(route).size)
        assertNotNull(store.rememberedResult(route))
        assertNull(store.loadResult(route))
        compose.onNodeWithText("Review Text Tools saved data").performScrollTo().performClick()
        compose.onNodeWithText("SAVED RESULT").assertExists()
        compose.onNodeWithText("SAVED HISTORY").assertExists()
        compose.onNodeWithText("Close review").performClick()
    }

    @Test fun routeClearRequiresConfirmationAndLeavesManualItemsAlone() {
        seed()
        assertTrue(store.record(Tools.ROUTE_PERCENTAGE, emptyMap(), "other history"))
        openSettings()
        compose.onNodeWithText("Clear Text Tools history").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(1, store.history(route).size)
        compose.onNodeWithText("Clear Text Tools history").performScrollTo().performClick()
        compose.onNodeWithText("Clear", substring = false).performClick()
        compose.waitUntil(5_000) { store.history(route).isEmpty() }
        assertNotNull(store.rememberedResult(route))
        assertEquals(1, store.history(Tools.ROUTE_PERCENTAGE).size)
        compose.onNodeWithText("Clear Text Tools remembered result").performScrollTo().performClick()
        compose.onNodeWithText("Clear", substring = false).performClick()
        compose.waitUntil(5_000) { store.rememberedResult(route) == null }
        assertEquals(1, store.presets().size)
        assertEquals(1, store.savedResults().size)
    }
}
