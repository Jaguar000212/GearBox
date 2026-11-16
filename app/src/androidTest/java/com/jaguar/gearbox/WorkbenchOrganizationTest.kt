package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.screens.WorkbenchScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WorkbenchOrganizationTest {
    @get:Rule val compose = createComposeRule()
    private val context = IsolatedToolTestContext()
    private val store = ToolMemoryStore(context)
    @After fun cleanup() { context.cleanup() }
    private fun seed(name: String, amount: String = "100") {
        assertTrue(store.saveToWorkbench(name, Tools.ROUTE_PERCENTAGE,
            mapOf("mode" to "OF_VALUE", "field1" to "10", "field2" to amount), "Original $name output"))
    }
    private fun show(presets: Boolean = false) {
        compose.setContent { context.Provide { MaterialTheme {
            WorkbenchScreen({}, { _, _, _ -> error("Organization must not restore a tool") }, store, presets)
        } } }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Select items") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    private fun select(name: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(name) and isToggleable())
        compose.onNode(hasText(name) and isToggleable()).performClick()
    }
    private fun header(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText(text).performScrollTo().performClick()
    }

    @Test fun renameKeepsOriginalInputsOutputAndIdentity() {
        seed("Before")
        val before = store.savedResults().single()
        show()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Rename"))
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithText("Saved name (1–80 characters)").performTextReplacement("After")
        compose.onNodeWithText("Save name").performClick()
        compose.waitUntil(5_000) { store.savedResults().single().name == "After" }
        assertEquals(before.copy(name = "After"), store.savedResults().single())
    }

    @Test fun selectionDeleteCanCancelAndDoesNotDeletePresets() {
        seed("First"); seed("Second")
        assertTrue(store.savePreset("Keep preset", Tools.ROUTE_PERCENTAGE, mapOf("mode" to "OF_VALUE")))
        show(); header("Select items"); select("First"); select("Second")
        header("Delete selected")
        compose.onNodeWithText("Delete 2 selected items?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(2, store.savedResults().size)
        header("Delete selected")
        compose.onNodeWithText("Delete", substring = false).performClick()
        compose.waitUntil(5_000) { store.savedResults().isEmpty() }
        assertEquals(1, store.presets().size)
    }

    @Test fun compatibleComparisonShowsSnapshotsWithoutMutation() {
        seed("First"); seed("Second", "200")
        val before = store.savedResults()
        show(); header("Select items"); select("First"); select("Second")
        header("Compare selected results")
        compose.onNodeWithText("Compare saved results").assertExists()
        compose.onNode(hasText("Original First output") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNode(hasText("Original Second output") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("Close comparison").performClick()
        assertEquals(before, store.savedResults())
        assertTrue(store.history(Tools.ROUTE_PERCENTAGE).isEmpty())
    }

    @Test fun filteringClearsSelectionAndPresetLaunchStartsOnPresets() {
        assertTrue(store.savePreset("Preset A", Tools.ROUTE_PERCENTAGE, mapOf("mode" to "OF_VALUE")))
        show(presets = true)
        compose.onNodeWithText("Presets (1)").assertIsSelected()
        header("Select items"); select("Preset A")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText("Find by saved name or tool").performTextInput("missing")
        compose.onNodeWithText("Cancel selection").assertDoesNotExist()
        compose.onNodeWithText("Delete selected").assertDoesNotExist()
        assertEquals(1, store.presets().size)
    }
    @Test fun overlongRenameIsNotTruncatedOrSaved() {
        seed("Before")
        show()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Rename"))
        compose.onNodeWithText("Rename").performClick()
        val longName = "x".repeat(81)
        compose.onNodeWithText("Saved name (1–80 characters)").performTextReplacement(longName)
        compose.onNode(hasSetTextAction() and hasText(longName)).assertExists()
        compose.onNodeWithText("Save name").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals("Before", store.savedResults().single().name)
    }

    @Test fun sensitiveSavedResultsCannotBeComparedAndBackCancelsSelection() {
        assertTrue(store.saveToWorkbench("Text A", Tools.ROUTE_TEXT_TOOLS, mapOf("input" to "sample"), "SAMPLE"))
        assertTrue(store.saveToWorkbench("Text B", Tools.ROUTE_TEXT_TOOLS, mapOf("input" to "sample2"), "SAMPLE2"))
        show(); header("Select items"); select("Text A"); select("Text B")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText("Compare selected results").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Cancel selection").assertDoesNotExist()
        assertEquals(2, store.savedResults().size)
    }

}
