package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.rememberToolHistory
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryObservationRestorationTest {
    @get:Rule val compose = createComposeRule()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(base.cacheDir, "observation-memory-${UUID.randomUUID()}")
    private val isolated = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir(): File = directory.also { it.mkdirs() }
    }
    @After fun cleanup() { directory.deleteRecursively() }

    @Test fun clearedCalculatedResultIsNotReplayedAfterRecreationButEditsRecord() {
        val route = Tools.ROUTE_COMPASS
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(historyEnabled = true))
        val input = mutableStateOf("1")
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalContext provides isolated) {
                rememberToolHistory(route, mapOf("input" to input.value), "Result ${input.value}", {},
                    recordingDelayMillis = 0L, restoreLastResult = false)
            }
        }
        compose.waitUntil(5_000) { store.history(route).size == 1 }
        assertTrue(store.clearHistory(route))
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        // Allow the old unconditional IO writer enough time to expose a replay.
        Thread.sleep(300)
        assertTrue(store.history(route).isEmpty())
        compose.runOnIdle { input.value = "2" }
        compose.waitUntil(5_000) { store.history(route).singleOrNull()?.output == "Result 2" }
    }

    @Test fun explicitRunIdentityStillRecordsIdenticalOutputs() {
        val route = Tools.ROUTE_COMPASS
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(historyEnabled = true))
        val run = mutableStateOf("first")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated) {
                rememberToolHistory(route, mapOf("_run" to run.value), "Same output", {},
                    recordingDelayMillis = 0L, restoreLastResult = false)
            }
        }
        compose.waitUntil(5_000) { store.history(route).size == 1 }
        compose.runOnIdle { run.value = "second" }
        compose.waitUntil(5_000) { store.history(route).size == 2 }
    }

    @Test fun clearedModeIsNotReplayedAfterRecreationButConfigurationChangesSave() {
        val route = Tools.ROUTE_SCREEN_RULER
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(rememberModes = true))
        val unit = mutableStateOf("cm")
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalContext provides isolated) {
                rememberToolHistory(route, emptyMap(), null, {},
                    modeInputs = mapOf("unit" to unit.value), restoreLastResult = false)
            }
        }
        compose.waitUntil(5_000) { store.loadMode(route)["unit"] == "cm" }
        assertTrue(store.clearRememberedModes())
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        Thread.sleep(300)
        assertTrue(store.loadMode(route).isEmpty())
        compose.runOnIdle { unit.value = "in" }
        compose.waitUntil(5_000) { store.loadMode(route)["unit"] == "in" }
    }


    @Test fun transientLoadingAfterRecreationDoesNotReplayClearedAsyncResult() {
        val route = Tools.ROUTE_COMPASS
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(historyEnabled = true))
        val output = mutableStateOf<String?>("Calculated output")
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalContext provides isolated) {
                rememberToolHistory(route, mapOf("input" to "unchanged"), output.value, {},
                    recordingDelayMillis = 0L, restoreLastResult = false)
            }
        }
        compose.waitUntil(5_000) { store.history(route).size == 1 }
        assertTrue(store.clearHistory(route))
        compose.runOnIdle { output.value = null }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.runOnIdle { output.value = "Calculated output" }
        compose.waitForIdle()
        Thread.sleep(300)
        assertTrue(store.history(route).isEmpty())
        compose.runOnIdle { output.value = "New output" }
        compose.waitUntil(5_000) { store.history(route).singleOrNull()?.output == "New output" }
    }

}
