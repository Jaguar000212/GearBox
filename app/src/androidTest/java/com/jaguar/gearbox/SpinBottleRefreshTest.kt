package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.SpinBottleScreen
import org.junit.Rule
import org.junit.Test

class SpinBottleRefreshTest {
    @get:Rule val rule = createComposeRule()

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
