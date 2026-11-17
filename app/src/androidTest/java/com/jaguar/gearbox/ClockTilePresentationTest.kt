package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.ClockState
import com.jaguar.gearbox.tiles.ClockTileLaunch
import com.jaguar.gearbox.timer.ClockStateStorage
import com.jaguar.gearbox.ui.tools.LocalClockNavigationTarget
import com.jaguar.gearbox.ui.tools.TimerStopwatchScreen
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ClockTilePresentationTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    // Suppress system alarm/notification effects: this test inspects isolated persisted state only.
    private val context = object : ContextWrapper(isolated) {
        override fun getApplicationContext(): Context = this
        override fun getSystemService(name: String): Any? =
            if (name == Context.NOTIFICATION_SERVICE || name == Context.ALARM_SERVICE) null else super.getSystemService(name)
    }
    @After fun cleanup() { isolated.cleanup() }

    @Test fun tileSelectsPresentationWithoutRestoringOrSavingClockMode() {
        val prefs = context.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
        val original = ClockState(mode = "STOPWATCH", stopwatchElapsedMillis = 3000, laps = listOf(1000, 3000), minutesInput = "7")
        ClockStateStorage(prefs).save(original)
        val before = prefs.all.toMap()
        val memory = ToolMemoryStore(context)
        memory.saveSettings(ToolMemorySettings(rememberModes = true))
        val savedMode = mapOf("mode" to "STOPWATCH", "minutes" to "19", "seconds" to "12")
        memory.saveMode(Tools.ROUTE_TIMER_STOPWATCH, savedMode)
        val target = mutableStateOf<String?>(ClockTileLaunch.TIMER.route)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalClockNavigationTarget provides target) {
                MaterialTheme { TimerStopwatchScreen({}) }
            }
        }
        compose.onNodeWithText("Timer", substring = false).assertIsSelected()
        compose.waitForIdle()
        Thread.sleep(300)
        assertEquals(before, prefs.all)
        assertEquals(savedMode, memory.loadMode(Tools.ROUTE_TIMER_STOPWATCH))
        compose.runOnIdle { target.value = "STOPWATCH" }
        compose.onNodeWithText("Stopwatch", substring = false).assertIsSelected()
        assertEquals(before, prefs.all)
    }
}
