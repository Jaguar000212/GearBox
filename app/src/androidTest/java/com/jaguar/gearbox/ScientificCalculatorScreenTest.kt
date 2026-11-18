package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.ScientificCalculatorScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScientificCalculatorScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = mapOf("expression" to " 2^3 ", "angleMode" to "DEG")
    private fun output() = isolated.getString(R.string.scientific_output, " 2^3 ", "DEG", "8.0")
    @Test fun explicitImportRequiresCalculateAndModeChangeClearsOutput() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_SCIENTIFIC_CALCULATOR, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { ScientificCalculatorScreen({}) }
        } } }
        compose.onNodeWithText(output()).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.scientific_calculate)).performScrollTo().performClick()
        compose.onNodeWithText(output()).assertExists()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_SCIENTIFIC_CALCULATOR, mapOf("angleMode" to "RAD"), modeOnly = true) }
        compose.onNodeWithText(output()).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.scientific_expression)).assertTextContains(" 2^3 ")
    }
    @Test fun rememberedResultHydratesWithoutReplayingHistory() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(rememberResults = true, historyEnabled = true)))
        assertTrue(store.saveResult(Tools.ROUTE_SCIENTIFIC_CALCULATOR, inputs, "old output"))
        compose.setContent { isolated.Provide { MaterialTheme { ScientificCalculatorScreen({}) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(output()).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        assertTrue(store.history(Tools.ROUTE_SCIENTIFIC_CALCULATOR).isEmpty())
    }
}
