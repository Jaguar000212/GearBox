package com.jaguar.gearbox

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.StopwatchSection
import com.jaguar.gearbox.ui.tools.TimerSection
import com.jaguar.gearbox.ui.tools.stopwatchResultText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClockActionsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun stopwatchResetCanBeCancelledThenConfirmed() {
        var resets = 0
        rule.setContent { MaterialTheme { Column {
            StopwatchSection(5000L, false, listOf(2000L), {}, {}, { resets++ })
        } } }
        rule.onNodeWithText("Reset").performClick()
        rule.onNodeWithText("Keep timing").performClick()
        rule.runOnIdle { assertEquals(0, resets) }
        rule.onNodeWithText("Reset").performClick()
        rule.onNodeWithText("Reset now").performClick()
        rule.runOnIdle { assertEquals(1, resets) }
    }

    @Test fun runningStopwatchCannotExportChangingResult() {
        rule.setContent { MaterialTheme { Column {
            StopwatchSection(5000L, true, emptyList(), {}, {}, {})
        } } }
        rule.onNodeWithText("Copy").assertIsNotEnabled()
        rule.onNodeWithText("Share").assertIsNotEnabled()
    }

    @Test fun activeTimerResetRequiresConfirmation() {
        var resets = 0
        rule.setContent { MaterialTheme { Column {
            TimerSection(10000L, true, false, "0", "10", {}, {}, {}, { resets++ })
        } } }
        rule.onNodeWithText("Reset").performClick()
        rule.onNodeWithText("Reset timer?").assertExists()
        rule.onNodeWithText("Keep timing").performClick()
        rule.runOnIdle { assertEquals(0, resets) }
    }

    @Test fun exportContainsChronologicalSplitAndTotalTimes() {
        assertEquals("Stopwatch: 00:05.00\nLap 1: split 00:02.00, total 00:02.00\nLap 2: split 00:03.00, total 00:05.00",
            stopwatchResultText(5000L, listOf(2000L, 5000L)))
    }
}
