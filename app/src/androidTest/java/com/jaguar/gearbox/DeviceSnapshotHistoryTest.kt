package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.OrientationSnapshot
import com.jaguar.gearbox.ui.components.OrientationState
import com.jaguar.gearbox.ui.components.OrientationStatus
import com.jaguar.gearbox.ui.tools.CompassScreen
import com.jaguar.gearbox.ui.tools.BubbleLevelScreen
import com.jaguar.gearbox.ui.tools.ScreenRulerScreen
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DeviceSnapshotHistoryTest {
    @get:Rule val compose = createComposeRule()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(base.cacheDir, "device-memory-${UUID.randomUUID()}")
    private val isolated = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir(): File = directory.also { it.mkdirs() }
    }
    @After fun cleanup() { directory.deleteRecursively() }

    @Test fun compassOnlyRecordsExplicitCaptureAndCannotRestorePhysicalReading() {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(historyEnabled = true))
        val orientation = mutableStateOf(OrientationState(OrientationSnapshot(0f, 0f, 0f), OrientationStatus.READY))
        compose.setContent { CompositionLocalProvider(LocalContext provides isolated) {
            MaterialTheme { CompassScreen({}, orientation.value) }
        } }
        compose.waitForIdle()
        assertTrue(store.history(Tools.ROUTE_COMPASS).isEmpty())
        compose.runOnIdle { orientation.value = OrientationState(OrientationSnapshot((Math.PI / 2).toFloat(), 0f, 0f), OrientationStatus.READY) }
        assertTrue(store.history(Tools.ROUTE_COMPASS).isEmpty())
        compose.onNodeWithText("Capture reading").performScrollTo().performClick()
        compose.waitUntil(5_000) { store.history(Tools.ROUTE_COMPASS).size == 1 }
        val captured = store.history(Tools.ROUTE_COMPASS).single().output
        assertTrue(captured.contains("Magnetic heading"))
        compose.runOnIdle { orientation.value = OrientationState(OrientationSnapshot(Math.PI.toFloat(), 0f, 0f), OrientationStatus.READY) }
        compose.waitForIdle()
        assertEquals(captured, store.history(Tools.ROUTE_COMPASS).single().output)
        compose.onNodeWithContentDescription("History").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(captured) and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Restore").assertCountEquals(0)
    }

    @Test fun unreliableLevelReadingCannotBeCaptured() {
        compose.setContent { CompositionLocalProvider(LocalContext provides isolated) {
            MaterialTheme { BubbleLevelScreen({}, OrientationState(OrientationSnapshot(0f, 0f, 0f), OrientationStatus.UNRELIABLE)) }
        } }
        compose.onNodeWithText("Capture reading").performScrollTo().assertIsNotEnabled()
    }

    @Test fun rulerRemembersOnlyUnitConfigurationWhenEnabled() {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(rememberModes = true))
        store.saveMode(Tools.ROUTE_SCREEN_RULER, mapOf("unit" to "in"))
        compose.setContent { CompositionLocalProvider(LocalContext provides isolated) {
            MaterialTheme { ScreenRulerScreen({}) }
        } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Inches · each small division is 1/16 inch").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(store.history(Tools.ROUTE_SCREEN_RULER).isEmpty())
        compose.onNodeWithText("Capture scale settings").assertExists()
    }
}
