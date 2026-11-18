package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.DecisionMatrixScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DecisionMatrixScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = mapOf("optionLabels" to "Option A\nOption B\nOption C", "criterionLabels" to "Value", "weights" to "2", "scores" to "5\n3\n1")
    @Test fun importsRequireCompareAndRemovalRequiresConfirmation() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_DECISION_MATRIX, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { DecisionMatrixScreen({}) }
        } } }
        compose.onNodeWithText("Weighted mean:", substring = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.matrix_compare)).performScrollTo().performClick()
        compose.onNodeWithText("Weighted mean:", substring = true).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.matrix_remove_option, 3)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.matrix_cancel)).performClick()
        compose.onNodeWithText(isolated.getString(R.string.matrix_option_label, 3)).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.matrix_remove_option, 3)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.matrix_remove)).performClick()
        compose.onNodeWithText(isolated.getString(R.string.matrix_option_label, 3)).assertDoesNotExist()
        compose.onNodeWithText("Weighted mean:", substring = true).assertDoesNotExist()
    }
    @Test fun sensitiveDefaultsDoNotStoreComparison() {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true))
        compose.setContent { isolated.Provide { MaterialTheme { DecisionMatrixScreen({}) } } }
        compose.onNodeWithText(isolated.getString(R.string.matrix_compare)).performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        assertTrue(store.history(Tools.ROUTE_DECISION_MATRIX).isEmpty())
        assertTrue(store.loadResult(Tools.ROUTE_DECISION_MATRIX) == null)
    }
}
