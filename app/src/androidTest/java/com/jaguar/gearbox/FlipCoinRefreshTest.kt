package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.FlipCoinScreen
import org.junit.Rule
import org.junit.Test

class FlipCoinRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun flipRevealsResultThenClearRestoresIdle() {
        rule.setContent { MaterialTheme { FlipCoinScreen {} } }
        rule.onNodeWithText("Copy").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Flip coin").performScrollTo().performClick()
        rule.onNodeWithText("Flip again").assertIsEnabled()
        rule.onNode(hasText("Heads") or hasText("Tails")).assertExists()
        rule.onNodeWithText("Share").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Ready to flip").assertExists()
        rule.onNodeWithText("Copy").assertIsNotEnabled()
    }

    @Test fun animationDisablesFlipAndResultActions() {
        rule.setContent { MaterialTheme { FlipCoinScreen {} } }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Flip coin").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(hasText("Flipping…") and hasClickAction()).assertIsNotEnabled()
        rule.onNodeWithText("Copy").assertIsNotEnabled()
        rule.mainClock.advanceTimeBy(1000)
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("Flip again").assertIsEnabled()
    }
}
