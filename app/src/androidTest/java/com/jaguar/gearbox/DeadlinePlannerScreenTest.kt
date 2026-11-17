package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.DeadlinePlannerScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeadlinePlannerScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = mapOf("deadline" to "2027-01-10", "asOf" to "2027-01-01", "ids" to "11,22", "names" to "Prepare\nReview", "durations" to "2,3")
    @Test fun explicitPlanningAndStableReorderAndConfirmedRemoval() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_DEADLINE_PLANNER, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { DeadlinePlannerScreen({}) }
        } } }
        compose.onNodeWithText("Earliest start:", substring = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.deadline_plan)).performScrollTo().performClick()
        compose.onNodeWithText("2027-01-06 through 2027-01-07", substring = true).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.deadline_up, 22)).performScrollTo().performClick()
        compose.onNodeWithText("Earliest start:", substring = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.deadline_duration, 22)).assertTextContains("3")
        compose.onNodeWithText(isolated.getString(R.string.deadline_plan)).performScrollTo().performClick()
        compose.onNodeWithText("2027-01-06 through 2027-01-08", substring = true).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.deadline_remove_phase, 11)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.deadline_cancel)).performClick()
        compose.onNodeWithText(isolated.getString(R.string.deadline_phase_label, 11)).assertExists()
    }
    @Test fun rememberedPlanUsesSavedAssessmentDateWithoutReplay() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true,
            automaticMemory = mapOf(Tools.ROUTE_DEADLINE_PLANNER to ToolAutomaticMemory(true, true)))))
        assertTrue(store.saveResult(Tools.ROUTE_DEADLINE_PLANNER, inputs, "old output"))
        compose.setContent { isolated.Provide { MaterialTheme { DeadlinePlannerScreen({}) } } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Assessment date: 2027-01-01", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1600); compose.waitForIdle(); Thread.sleep(1500)
        assertTrue(store.history(Tools.ROUTE_DEADLINE_PLANNER).isEmpty())
    }
}
