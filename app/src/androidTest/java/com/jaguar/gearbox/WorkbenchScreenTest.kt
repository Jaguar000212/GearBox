package com.jaguar.gearbox

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.ui.screens.WorkbenchScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class WorkbenchScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val directory = File(InstrumentationRegistry.getInstrumentation().context.cacheDir,
        "workbench_ui_test_${UUID.randomUUID()}")
    private val store = ToolMemoryStore.inDirectory(directory)
    @After fun cleanup() { directory.deleteRecursively() }

    @Test fun restoringExplicitResultPassesItsInputs() {
        store.saveToWorkbench("My estimate", "tool/percentage", mapOf("amount" to "100"), "10")
        var restored: Pair<String, Map<String, String>>? = null
        compose.setContent {
            MaterialTheme { WorkbenchScreen({}, { route, inputs, _ -> restored = route to inputs }, store) }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("My estimate").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Restore").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("tool/percentage" to mapOf("amount" to "100"), restored) }
    }

    @Test fun presetRestoreIsMarkedAsConfigurationOnly() {
        store.savePreset("Color display", "tool/color_picker", mapOf("view" to "RGB"))
        var modeOnly = false
        var imported: Map<String, String>? = null
        compose.setContent {
            MaterialTheme { WorkbenchScreen({}, { _, inputs, isPreset ->
                imported = inputs
                modeOnly = isPreset
            }, store) }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Presets (1)").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Presets (1)").performClick()
        compose.onNodeWithText("Use preset").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(modeOnly)
            assertEquals(mapOf("view" to "RGB"), imported)
        }
    }

    @Test fun cancelingDeleteLeavesSavedResultIntact() {
        store.saveToWorkbench("Keep me", "tool/percentage", emptyMap(), "10")
        compose.setContent { MaterialTheme { WorkbenchScreen({}, { _, _, _ -> }, store) } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Keep me").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals("Keep me", store.savedResults().single().name)
    }
    @Test fun privateShareRequiresReviewAndCancelLeavesSavedItem() {
        store.saveToWorkbench("Private example", "tool/text_tools", mapOf("text" to "example private text"), "EXAMPLE PRIVATE TEXT")
        compose.setContent { MaterialTheme { WorkbenchScreen({}, { _, _, _ -> }, store) } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Private example").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Share").performScrollTo().performClick()
        compose.onNodeWithText("Review before sharing").assertIsDisplayed()
        compose.onNodeWithText("The external app you choose will receive this text. It may contain secrets or private information. Cancel if you need to remove anything first.").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Review before sharing").assertDoesNotExist()
        assertEquals("Private example", store.savedResults().single().name)
    }

}
