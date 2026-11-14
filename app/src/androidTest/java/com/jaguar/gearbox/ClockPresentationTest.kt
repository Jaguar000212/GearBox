package com.jaguar.gearbox

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jaguar.gearbox.ui.tools.StopwatchSection
import com.jaguar.gearbox.ui.tools.TimerSection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClockPresentationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun pausedStopwatchShowsResumeAndLapSplits() {
        rule.setContent { MaterialTheme { Column {
            StopwatchSection(5000L, false, listOf(2000L, 5000L), {}, {}, {})
        } } }
        rule.onNodeWithText("Paused").assertExists()
        rule.onNodeWithText("Resume").assertExists()
        rule.onNodeWithText("Split 00:03.00").assertExists()
        rule.onNodeWithText("Total 00:05.00").assertExists()
    }

    @Test fun completedTimerShowsRestartAndPresetsUpdateDraft() {
        var minutes = "0"
        var seconds = "10"
        rule.setContent { MaterialTheme { Column {
            TimerSection(0L, false, true, minutes, seconds,
                { minutes = it }, { seconds = it }, {}, {})
        } } }
        rule.onNodeWithText("Time's up!").assertExists()
        rule.onNodeWithText("Start again").assertExists()
        rule.onNodeWithText("5 min").performClick()
        rule.runOnIdle { assertEquals("5", minutes); assertEquals("0", seconds) }
    }

    @Test fun pausedTimerDoesNotOfferDurationPresets() {
        rule.setContent { MaterialTheme { Column {
            TimerSection(10000L, false, false, "0", "10", {}, {}, {}, {})
        } } }
        rule.onNodeWithText("Resume").assertExists()
        rule.onNodeWithText("5 min").assertDoesNotExist()
    }
}
