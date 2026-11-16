package com.jaguar.gearbox

import android.content.Context
import java.util.UUID
import org.junit.After
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.DeviceCapabilities
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.screens.ActiveClockShortcut
import com.jaguar.gearbox.ui.screens.DiscoveryToolRow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeStatusTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val preferencesName = "home_status_${UUID.randomUUID()}"
    @After fun cleanup() { context.deleteSharedPreferences(preferencesName) }

    @Test fun unsupportedSensorHintDoesNotDisableTool() {
        val tool = Tools.byRoute(Tools.ROUTE_COMPASS)!!
        val capabilities = DeviceCapabilities(orientationSensor = false, cameraFlash = false)
        val hint = capabilities.hint(tool.route)!!
        var opened = false
        val favorites = FavoritesStore(context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE))
        compose.setContent {
            MaterialTheme {
                DiscoveryToolRow(tool, favorites, { opened = true }, false, hint)
            }
        }
        compose.onNodeWithText(hint).assertIsDisplayed()
        compose.onNodeWithText(tool.name).performClick()
        compose.runOnIdle { assertTrue(opened) }
        assertTrue(capabilities.hint(Tools.ROUTE_FLASHLIGHT)!!.contains("unavailable"))
        assertTrue(capabilities.hint(Tools.ROUTE_BUBBLE_LEVEL)!!.contains("unavailable"))
        assertNull(capabilities.hint(Tools.ROUTE_AVERAGE))
    }

    @Test fun shortcutOpensWithoutAnyClockControl() {
        var opens = 0
        compose.setContent {
            MaterialTheme { ActiveClockShortcut("Timer paused · Stopwatch running") { opens++ } }
        }
        compose.onNodeWithText("Timer paused · Stopwatch running").assertIsDisplayed()
        compose.onNodeWithText("Return to clocks").performClick()
        compose.runOnIdle { assertEquals(1, opens) }
        compose.onNodeWithText("Pause", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Resume", substring = false).assertDoesNotExist()
    }
}
