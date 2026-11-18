package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.TextDiffScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TextDiffScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = mapOf("firstText" to "a\r\nb", "secondText" to "a\r\nb\r\n")
    private fun counts() = isolated.getString(R.string.text_diff_counts, 1, 0, 2)
    @Test fun importIsInputOnlyAndSameValueRestoreClearsComparison() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_TEXT_DIFF, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { TextDiffScreen({}) }
        } } }
        compose.onNodeWithText(counts()).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.text_diff_compare)).performScrollTo().performClick()
        compose.onNodeWithText(counts()).assertExists()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_TEXT_DIFF, inputs) }
        compose.onNodeWithText(counts()).assertDoesNotExist()
    }
    @Test fun privateTextDoesNotAutosaveByDefaultAndLimitsDisableCompare() {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true))
        compose.setContent { isolated.Provide { MaterialTheme { TextDiffScreen({}) } } }
        compose.onNodeWithText(isolated.getString(R.string.text_diff_first)).performTextReplacement("original private text")
        compose.onNodeWithText(isolated.getString(R.string.text_diff_compare)).performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        assertTrue(store.history(Tools.ROUTE_TEXT_DIFF).isEmpty())
        assertTrue(store.loadResult(Tools.ROUTE_TEXT_DIFF) == null)
        compose.onNodeWithText(isolated.getString(R.string.text_diff_first)).performScrollTo().performTextReplacement("\n".repeat(200))
        compose.onNodeWithText(isolated.getString(R.string.text_diff_compare)).performScrollTo().assertIsNotEnabled()
    }
    @Test fun whitespaceModePreservesBothVersionsAndChangingModeClearsResult() {
        val first = "  hello\tworld "
        val second = "hello world"
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_TEXT_DIFF,
            mapOf("firstText" to first, "secondText" to second, "mode" to "IGNORE_ASCII_WHITESPACE")))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { TextDiffScreen({}) }
        } } }
        val equivalent = isolated.getString(R.string.text_diff_equivalent_line, first, second)
        compose.onNodeWithText(equivalent).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.text_diff_compare)).performScrollTo().performClick()
        compose.onNodeWithText(equivalent).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.shared_label_value,
            isolated.getString(R.string.text_diff_mode), isolated.getString(R.string.text_diff_ignore_ascii)))
            .performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.text_diff_exact), useUnmergedTree = true).performClick()
        compose.onNodeWithText(equivalent).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.text_diff_compare)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.text_diff_counts, 1, 1, 0)).assertExists()
        // Legacy two-field imports explicitly restore Exact, not the remembered alternate mode.
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_TEXT_DIFF,
            mapOf("firstText" to first, "secondText" to second)) }
        compose.onNodeWithText(isolated.getString(R.string.text_diff_counts, 1, 1, 0)).assertDoesNotExist()
    }

}
