package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.BillSplitterScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BillSplitterScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = mapOf("bill" to "10.00", "tax" to "0.0", "tipPercent" to "0.00", "diners" to "3")
    private fun output(): String {
        val totals = isolated.getString(R.string.bill_split_totals, "10.00", "0.00", "0.00", "10.00")
        val shares = isolated.getString(R.string.bill_split_first_one, "3.34") + "\n" +
            isolated.getString(R.string.bill_split_remaining_many, 2, 3, "3.33")
        return isolated.getString(R.string.bill_split_export, totals, shares)
    }
    @Test fun explicitRestoreNeedsCalculateAndEditClearsResult() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_BILL_SPLITTER, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { BillSplitterScreen({}) }
        } } }
        compose.onNodeWithText(output()).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.bill_split_calculate)).performScrollTo().performClick()
        compose.onNodeWithText(output()).assertExists()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_BILL_SPLITTER, inputs) }
        compose.onNodeWithText(output()).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.bill_split_calculate)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.bill_split_diners)).performScrollTo().performTextReplacement("0")
        compose.onNodeWithText(output()).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.bill_split_calculate)).assertIsNotEnabled()
    }
    @Test fun rememberedOutputReconstructsWithoutHistoryReplay() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(rememberResults = true, historyEnabled = true)))
        assertTrue(store.saveResult(Tools.ROUTE_BILL_SPLITTER, inputs, "old formatted output"))
        compose.setContent { isolated.Provide { MaterialTheme { BillSplitterScreen({}) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(output()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("old formatted output").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        assertTrue(store.history(Tools.ROUTE_BILL_SPLITTER).isEmpty())
    }
}
