package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.SpinBottleScreen
import org.junit.Rule
import org.junit.Test

class SpinBottleRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun completedAngleSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { MaterialTheme { SpinBottleScreen {} } }
        rule.onNodeWithText("Spin").performScrollTo().performClick()
        rule.onNodeWithText("Spin again").assertIsEnabled()
        val status = rule.onNodeWithText("Bottle points", substring = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText(status).assertExists()
        rule.onNodeWithText("Spin again").assertIsEnabled()
    }

    @Test fun interruptedFirstSpinRestoresReadyWithoutAResult() {
        assertInterruptedSpinRestoresReady(completePreviousSpin = false)
    }

    @Test fun interruptedSecondSpinDoesNotRestorePreviousResult() {
        assertInterruptedSpinRestoresReady(completePreviousSpin = true)
    }

    private fun assertInterruptedSpinRestoresReady(completePreviousSpin: Boolean) {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { MaterialTheme { SpinBottleScreen {} } }
        if (completePreviousSpin) {
            rule.onNodeWithText("Spin").performScrollTo().performClick()
            rule.onNodeWithText("Spin again").assertIsEnabled()
            rule.onNodeWithText("Bottle points", substring = true).assertExists()
        }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(if (completePreviousSpin) "Spin again" else "Spin")
            .performScrollTo().performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(hasText("Spinning…") and hasClickAction()).assertIsNotEnabled()
        restoration.emulateSavedInstanceStateRestore()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Ready to spin").assertExists()
        rule.onNodeWithText("Bottle points", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Spin").assertIsEnabled()
        rule.mainClock.autoAdvance = true
    }

    @Test fun spinLocksUntilAnimationFinishesThenAllowsAnotherSpin() {
        rule.mainClock.autoAdvance = false
        rule.setContent { MaterialTheme { SpinBottleScreen {} } }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Ready to spin").assertExists()
        rule.onNodeWithText("Spin").performScrollTo().performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(hasText("Spinning…") and hasClickAction()).assertIsNotEnabled()
        rule.mainClock.advanceTimeBy(3000)
        rule.onNodeWithText("Spin again").assertIsEnabled().performClick()
        rule.mainClock.advanceTimeBy(3000)
        rule.onNodeWithText("Spin again").assertIsEnabled()
    }
}
