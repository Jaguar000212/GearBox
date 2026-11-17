package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.LocalToolImport
import com.jaguar.gearbox.ui.components.LocalConsumeToolImport
import com.jaguar.gearbox.ui.components.ToolImport
import com.jaguar.gearbox.ui.tools.TextToolsScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class TextEditorUndoTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private fun action(label: String) = compose.onNodeWithText(label, substring = false).performScrollTo().performClick()
    private fun input(value: String) = compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement(value)

    @Test fun editsCaseAndClearCanBeUndoneAndNewEditDropsRedo() {
        compose.setContent { isolated.Provide { MaterialTheme { TextToolsScreen({}) } } }
        compose.onNodeWithText("Undo edit").assertIsNotEnabled()
        input("Hello world")
        action("Convert to: Title Case"); action("UPPERCASE")
        action("Clear")
        action("Undo edit")
        compose.onNode(hasSetTextAction()).assertTextContains("Hello world")
        compose.onNodeWithText("Convert to: UPPERCASE").assertExists()
        action("Undo edit")
        compose.onNodeWithText("Convert to: Title Case").assertExists()
        action("Redo edit")
        compose.onNodeWithText("Convert to: UPPERCASE").assertExists()
        action("Undo edit")
        input("Different")
        compose.onNodeWithText("Redo edit").assertIsNotEnabled()
    }

    @Test fun recreationKeepsCurrentDraftButNotPriorTextStack() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { isolated.Provide { MaterialTheme { TextToolsScreen({}) } } }
        input("Earlier"); input("Current")
        compose.onNodeWithText("Undo edit").assertIsEnabled()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextContains("Current")
        compose.onNodeWithText("Undo edit").assertIsNotEnabled()
        compose.onNodeWithText("Redo edit").assertIsNotEnabled()
    }

    @Test fun explicitInputAndModeImportsResetPriorEdits() {
        val pending = mutableStateOf<ToolImport?>(null)
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { TextToolsScreen({}) }
        } } }
        input("Private old draft")
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_TEXT_TOOLS, mapOf("text" to "Imported", "case" to "LOWER")) }
        compose.waitUntil(5_000) { pending.value == null }
        compose.onNode(hasSetTextAction()).assertTextContains("Imported")
        compose.onNodeWithText("Undo edit").assertIsNotEnabled()
        input("Edited import")
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_TEXT_TOOLS, mapOf("case" to "UPPER"), modeOnly = true) }
        compose.waitUntil(5_000) { pending.value == null }
        compose.onNode(hasSetTextAction()).assertTextContains("Edited import")
        compose.onNodeWithText("Undo edit").assertIsNotEnabled()
        compose.onNodeWithText("Redo edit").assertIsNotEnabled()
    }
}
