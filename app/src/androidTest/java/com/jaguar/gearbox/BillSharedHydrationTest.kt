package com.jaguar.gearbox

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BillSharedHydrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = IsolatedToolTestContext()
    private val store = ToolMemoryStore(context)
    private val route = "tool/bill_splitter"
    private val simple = mapOf("bill" to "100", "tax" to "0", "tipPercent" to "10", "diners" to "3")
    private val shared = SharedBillSnapshot(listOf(SharedParticipant(1, "A"), SharedParticipant(2, "B")),
        listOf(SharedExpense(1, 100, 1, setOf(1, 2)))).toInputs()
    @After fun cleanup() { context.cleanup() }

    private fun verify(initial: Map<String, String>, restored: Map<String, String>) {
        store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true, rememberModes = true,
            automaticMemory = mapOf(route to ToolAutomaticMemory(true, true))))
        assertTrue(store.saveResult(route, restored, "completed"))
        val originalResult = store.rememberedResult(route)
        assertTrue(store.clearHistory(route))
        assertTrue(store.clearRememberedModes())
        compose.setContent {
            context.Provide {
                var inputs by remember { mutableStateOf(initial) }
                var output by remember { mutableStateOf<String?>(null) }
                fun modes(values: Map<String, String>) = if (values["format"] == "shared-v1")
                    mapOf("view" to "SHARED") else mapOf("diners" to values.getValue("diners"))
                rememberToolHistory(route, inputs, output,
                    onRestore = { inputs = it; output = null },
                    onRestoreLastResult = { inputs = it; output = "completed" },
                    modeInputs = modes(inputs), onRestoreMode = {},
                    restoredModeInputs = ::modes,
                    resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false },
                    recordingDelayMillis = 10)
                Text(if (output == null) "Loading" else "Hydrated")
            }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Hydrated").fetchSemanticsNodes().isNotEmpty() }
        // Allow queued persistence work well beyond the deliberately short recording debounce.
        Thread.sleep(250)
        compose.waitForIdle()
        assertTrue(store.loadMode(route).isEmpty())
        assertTrue(store.history(route).isEmpty())
        assertEquals(originalResult, store.rememberedResult(route))
    }
    private fun verifyExplicitRestoreAfterHydration(useImport: Boolean, invalid: Boolean = false) {
        store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true,
            automaticMemory = mapOf(route to ToolAutomaticMemory(true, true))))
        store.saveResult(route, simple, "completed")
        store.record(route, simple, "completed")
        compose.setContent {
            context.Provide {
                var inputs by remember { mutableStateOf(simple) }
                var output by remember { mutableStateOf<String?>(null) }
                var pending by remember { mutableStateOf<ToolImport?>(null) }
                CompositionLocalProvider(LocalToolImport provides pending, LocalConsumeToolImport provides { pending = null }) {
                    val binding = rememberToolHistory(route, inputs, output,
                        onRestore = { if (parseBillSplitterSnapshot(it) != null) { inputs = it; output = null } },
                        canResetHydratedCaptureOnRestore = { parseBillSplitterSnapshot(it) != null || parseSharedBillSnapshot(it) != null },
                        onRestoreLastResult = { inputs = it; output = "completed" },
                        resultRequiresExplicitAction = true, restoredResultRequiresExplicitAction = { false },
                        recordingDelayMillis = 10)
                    Column {
                        Text(if (output == null) "Waiting for Calculate" else "Completed")
                        Button(onClick = {
                            val values = if (invalid) mapOf("wrongShape" to "small") else simple
                            if (useImport) pending = ToolImport(route, values) else binding.restore(values)
                        }) { Text("Explicit restore") }
                        Button(onClick = { output = "completed" }) { Text("Calculate") }
                    }
                }
            }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Completed").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(store.clearHistory(route))
        Thread.sleep(100)
        assertTrue(store.history(route).isEmpty()) // hydration alone must not replay after clear
        compose.onNodeWithText("Explicit restore").performClick()
        if (invalid) {
            compose.onNodeWithText("Completed").assertExists()
            Thread.sleep(250)
            compose.waitForIdle()
            assertTrue(store.history(route).isEmpty())
            return
        }
        compose.onNodeWithText("Waiting for Calculate").assertExists()
        Thread.sleep(100)
        assertTrue(store.history(route).isEmpty()) // explicit restore is still inputs-only
        compose.onNodeWithText("Calculate").performClick()
        compose.waitUntil(5000) { store.history(route).size == 1 }
        Thread.sleep(100)
        assertEquals(1, store.history(route).size)
        assertEquals(simple, store.history(route).single().inputs)
    }
    @Test fun historyRestoreOfHydratedInputsCanRecordAfterExplicitCalculate() = verifyExplicitRestoreAfterHydration(false)
    @Test fun fullImportOfHydratedInputsCanRecordAfterExplicitCalculate() = verifyExplicitRestoreAfterHydration(true)

    @Test fun invalidFullImportDoesNotReleaseHydratedCapture() = verifyExplicitRestoreAfterHydration(true, true)
    @Test fun invalidHistoryRestoreDoesNotReleaseHydratedCapture() = verifyExplicitRestoreAfterHydration(false, true)

    @Test fun simpleToSharedDoesNotRecreateClearedModeOrHistory() = verify(simple, shared)
    @Test fun sharedToLegacySimpleDoesNotRecreateClearedModeOrHistory() = verify(shared, simple)
}
