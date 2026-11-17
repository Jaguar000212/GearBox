package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.AspectRatioScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AspectRatioScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = aspectRatioInputs("WIDTH", "1920", "1080", "1280", "", "")
    @Test fun explicitImportNeedsCalculationAndRatioModeNeedsNoTargets() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_ASPECT_RATIO, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { AspectRatioScreen({}) }
        } } }
        compose.onNodeWithText("1280 × 720", substring = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.aspect_calculate)).performScrollTo().performClick()
        compose.onNodeWithText("1280 × 720", substring = true).assertExists()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_ASPECT_RATIO, mapOf("mode" to "RATIO"), modeOnly = true) }
        compose.onNodeWithText("1280 × 720", substring = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.aspect_target_width)).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.aspect_calculate)).performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText(isolated.getString(R.string.aspect_ratio_output, 16, 9, 1920, 1080)).assertExists()
    }
    @Test fun rememberedPercentResultKeepsLexicalSnapshotAndDoesNotReplayHistory() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(rememberResults = true, historyEnabled = true)))
        assertTrue(store.saveResult(Tools.ROUTE_ASPECT_RATIO, aspectRatioInputs("PERCENT", "10", "10", "", "", "50.00"), "old output"))
        compose.setContent { isolated.Provide { MaterialTheme { AspectRatioScreen({}) } } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("5 × 5", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1600); compose.waitForIdle(); Thread.sleep(1500)
        assertTrue(store.history(Tools.ROUTE_ASPECT_RATIO).isEmpty())
    }
}
