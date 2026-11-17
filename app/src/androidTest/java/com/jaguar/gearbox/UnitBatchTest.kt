package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.UnitConverterScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UnitBatchTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    @Test fun savedBatchHydratesInputsAndExplicitConvertCanRecordAgain() {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true))
        store.saveResult(Tools.ROUTE_UNIT_CONVERTER, mapOf("format" to "batch-v1", "batchInput" to "1\nbad",
            "category" to "LENGTH", "fromUnit" to "Meters", "toUnit" to "Centimeters"), "old batch")
        compose.setContent { isolated.Provide { MaterialTheme { UnitConverterScreen({}) } } }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Numbers, one per line") and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Numbers, one per line").assertTextContains("1\nbad")
        compose.onNodeWithText("old batch").assertDoesNotExist()
        assertTrue(store.history(Tools.ROUTE_UNIT_CONVERTER).isEmpty())
        compose.onNodeWithText("Convert batch").performScrollTo().performClick()
        compose.waitUntil(5_000) { store.history(Tools.ROUTE_UNIT_CONVERTER).any { it.inputs["format"] == "batch-v1" } }
        val saved = store.history(Tools.ROUTE_UNIT_CONVERTER).last()
        assertTrue(saved.output.contains("Line 1: 1 m = 100 cm"))
        assertTrue(saved.output.contains("Line 2: “bad” — Invalid finite number"))
        assertEquals("1\nbad", saved.inputs["batchInput"])
    }

    @Test fun reverseUndoAndImportResetAreEditorOnly() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_UNIT_CONVERTER,
            mapOf("category" to "LENGTH", "fromUnit" to "Meters", "toUnit" to "Centimeters", "input" to "2")))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value, LocalConsumeToolImport provides { pending.value = null }) { UnitConverterScreen({}) }
        } } }
        compose.onNodeWithText("Value").assertTextContains("2")
        compose.onNodeWithText("Reverse this conversion").performScrollTo().performClick()
        compose.onNodeWithText("Value").assertTextContains("200.0")
        compose.onNodeWithText("Undo edit").performScrollTo().performClick()
        compose.onNodeWithText("Value").assertTextContains("2")
        compose.onNodeWithText("Redo edit").performClick()
        compose.onNodeWithText("Value").assertTextContains("200.0")
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_UNIT_CONVERTER,
            mapOf("category" to "LENGTH", "fromUnit" to "Meters", "toUnit" to "Centimeters", "input" to "3")) }
        compose.onNodeWithText("Value").assertTextContains("3")
        compose.onNodeWithText("Undo edit").assertIsNotEnabled()
        compose.onNodeWithText("Redo edit").assertIsNotEnabled()
    }
    @Test fun restoredTooManyLinesRemainVisibleAndCannotConvert() {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true))
        val text = List(101) { "1" }.joinToString("\n")
        assertTrue(store.saveResult(Tools.ROUTE_UNIT_CONVERTER, mapOf("format" to "batch-v1", "batchInput" to text,
            "category" to "LENGTH", "fromUnit" to "Meters", "toUnit" to "Centimeters"), "legacy malformed batch"))
        compose.setContent { isolated.Provide { MaterialTheme { UnitConverterScreen({}) } } }
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Numbers, one per line") and hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Numbers, one per line").assertTextContains(text)
        compose.onNodeWithText(isolated.getString(R.string.unit_batch_too_many)).assertExists()
        compose.onNodeWithText("Convert batch").performScrollTo().performClick()
        compose.onNodeWithText("Numbers, one per line").assertTextContains(text)
        compose.onNodeWithText(isolated.getString(R.string.unit_batch_too_many)).assertExists()
        compose.onNodeWithText("legacy malformed batch").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        assertTrue(store.history(Tools.ROUTE_UNIT_CONVERTER).isEmpty())
    }

}
