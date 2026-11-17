package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.ToolMemorySettings
import org.junit.Assert.assertTrue
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.ColorBlendScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ColorBlendScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    @Test fun explicitImportRequiresBlendAndEditingInvalidatesOutput() {
        val inputs = mapOf("firstHex" to "#000000", "secondHex" to "#FFFFFF", "weight" to "50")
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_COLOR_BLEND, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { ColorBlendScreen({}) }
        } } }
        compose.onNodeWithText(isolated.getString(R.string.blend_first)).assertTextContains("#000000")
        val result = isolated.getString(R.string.blend_output, "#808080", 128, 128, 128)
        compose.onNodeWithText(result).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.blend_action)).performScrollTo().performClick()
        compose.onNodeWithText(result).assertExists()
        // Same-value explicit restoration must clear a generated result too.
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_COLOR_BLEND, inputs) }
        compose.onNodeWithText(result).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.blend_action)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.blend_weight)).performScrollTo().performTextReplacement("101")
        compose.onNodeWithText(result).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.blend_action)).assertIsNotEnabled()
    }

    @Test fun rememberedCompletedResultHydratesWithoutCreatingHistory() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(rememberResults = true, historyEnabled = true)))
        assertTrue(store.saveResult(Tools.ROUTE_COLOR_BLEND,
            mapOf("firstHex" to "#abcdef", "secondHex" to "#abcdef", "weight" to "50"), "legacy display text"))
        compose.setContent { isolated.Provide { MaterialTheme { ColorBlendScreen({}) } } }
        val result = isolated.getString(R.string.blend_output, "#ABCDEF", 171, 205, 239)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(result).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("legacy display text").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        assertTrue(store.history(Tools.ROUTE_COLOR_BLEND).isEmpty())
    }

    @Test fun alphaAndShorthandAreRejected() {
        compose.setContent { isolated.Provide { MaterialTheme { ColorBlendScreen({}) } } }
        compose.onNodeWithText(isolated.getString(R.string.blend_first)).performTextReplacement("#80FF0000")
        compose.onNodeWithText(isolated.getString(R.string.blend_action)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(isolated.getString(R.string.blend_first)).performScrollTo().performTextReplacement("#F00")
        compose.onNodeWithText(isolated.getString(R.string.blend_action)).performScrollTo().assertIsNotEnabled()
    }
}
