package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.BusinessDaysScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BusinessDaysScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = mapOf("mode" to "ADD", "start" to "2026-11-20", "end" to "2026-11-23", "offset" to "1",
        "weekendMask" to "96", "excludedDates" to " 2026-11-23\n2026-11-23 ")
    private fun output() = isolated.getString(R.string.business_result_date, "2026-11-24", "2026-11-20", 1)
    @Test fun explicitImportRequiresCalculateAndSameInputRestoreClearsResult() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_BUSINESS_DAY_PLANNER, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { BusinessDaysScreen({}) }
        } } }
        compose.onNodeWithText(output()).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.business_calculate)).performScrollTo().performClick()
        compose.onNodeWithText(output()).assertExists()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_BUSINESS_DAY_PLANNER, inputs) }
        compose.onNodeWithText(output()).assertDoesNotExist()
    }
    @Test fun rememberedResultPreservesRawExclusionsAndDoesNotReplayHistory() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(rememberResults = true, historyEnabled = true)))
        assertTrue(store.saveResult(Tools.ROUTE_BUSINESS_DAY_PLANNER, inputs, "old result"))
        compose.setContent { isolated.Provide { MaterialTheme { BusinessDaysScreen({}) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(output()).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        assertTrue(store.history(Tools.ROUTE_BUSINESS_DAY_PLANNER).isEmpty())
    }
}
