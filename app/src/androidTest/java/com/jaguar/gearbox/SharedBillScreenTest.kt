package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.BillSplitterScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SharedBillScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val shared = SharedBillSnapshot(listOf(SharedParticipant(1, "A"), SharedParticipant(2, "B"), SharedParticipant(9, "C")),
        listOf(SharedExpense(7, 3000, 1, setOf(1, 2)))).toInputs()
    @Test fun crossModeImportsAndStableParticipantRemoval() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_BILL_SPLITTER, shared))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { BillSplitterScreen({}) }
        } } }
        val transfer = isolated.getString(R.string.shared_bill_transfer, 2, "B", 1, "A", "15.00")
        compose.onNodeWithText(transfer, substring = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_calculate)).performScrollTo().performClick()
        compose.onNodeWithText(transfer, substring = true).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_remove_person, 1)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_remove_person, 9)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_cancel)).performClick()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_person_label, 9)).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_remove_person, 9)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_remove)).performClick()
        compose.onNodeWithText(transfer, substring = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_add_person)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_person_label, 10)).assertExists()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_BILL_SPLITTER,
            mapOf("bill" to "10", "tax" to "0", "tipPercent" to "0", "diners" to "2")) }
        compose.onNodeWithText(isolated.getString(R.string.bill_split_bill)).assertTextContains("10")
        compose.onNodeWithText(isolated.getString(R.string.shared_bill_calculate)).assertDoesNotExist()
        compose.onNodeWithText(transfer, substring = true).assertDoesNotExist()
    }
    @Test fun sharedHydrationDoesNotRecreateClearedHistoryOrModes() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true, rememberModes = true,
            automaticMemory = mapOf(Tools.ROUTE_BILL_SPLITTER to ToolAutomaticMemory(historyEnabled = true, rememberResults = true)))))
        assertTrue(store.saveResult(Tools.ROUTE_BILL_SPLITTER, shared, "old display"))
        store.clearRememberedModes()
        compose.setContent { isolated.Provide { MaterialTheme { BillSplitterScreen({}) } } }
        val transfer = isolated.getString(R.string.shared_bill_transfer, 2, "B", 1, "A", "15.00")
        compose.waitUntil(5_000) { compose.onAllNodesWithText(transfer, substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1600)
        compose.waitForIdle()
        Thread.sleep(1500)
        assertTrue(store.history(Tools.ROUTE_BILL_SPLITTER).isEmpty())
        assertTrue(store.loadMode(Tools.ROUTE_BILL_SPLITTER).isEmpty())
    }
}
