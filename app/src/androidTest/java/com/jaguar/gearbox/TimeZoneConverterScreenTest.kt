package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.TimeZoneConverterScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TimeZoneConverterScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = mapOf("date" to "2026-11-01", "time" to "01:30", "sourceZone" to "America/New_York",
        "targetZone" to "UTC", "sourceOffset" to "AUTO")
    @Test fun overlapRequiresChoiceAndEditingSourceInvalidatesIt() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_TIME_ZONE_CONVERTER, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { TimeZoneConverterScreen({}) }
        } } }
        compose.onNodeWithText(isolated.getString(R.string.zone_convert)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(isolated.getString(R.string.shared_label_value,
            isolated.getString(R.string.zone_offset), isolated.getString(R.string.zone_choose_offset))).performScrollTo().performClick()
        compose.onNode(hasText(isolated.getString(R.string.zone_offset_value, "-04:00")) and isSelectable()).performClick()
        compose.onNodeWithText(isolated.getString(R.string.zone_convert)).performScrollTo().performClick()
        compose.onNodeWithText("2026-11-01 05:30:00", substring = true).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.zone_time)).performScrollTo().performTextReplacement("01:31")
        compose.onNodeWithText("2026-11-01 05:30:00", substring = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.zone_convert)).performScrollTo().assertIsNotEnabled()
    }
    @Test fun rememberedResultPreservesChoiceWithoutHistoryReplay() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(rememberResults = true, historyEnabled = true)))
        assertTrue(store.saveResult(Tools.ROUTE_TIME_ZONE_CONVERTER, inputs + ("sourceOffset" to "-05:00"), "old result"))
        compose.setContent { isolated.Provide { MaterialTheme { TimeZoneConverterScreen({}) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("2026-11-01 06:30:00", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1_600)
        compose.waitForIdle()
        Thread.sleep(1_500)
        assertTrue(store.history(Tools.ROUTE_TIME_ZONE_CONVERTER).isEmpty())
    }
}
