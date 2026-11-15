package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.DiceRollScreen
import org.junit.Rule
import org.junit.Test

class DiceRollRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun countChangeInvalidatesRollAndActions() {
        rule.setContent { MaterialTheme { DiceRollScreen {} } }
        rule.onNodeWithContentDescription("Fewer dice").assertIsNotEnabled()
        rule.onNodeWithText("Roll").performScrollTo().performClick()
        rule.onNodeWithText("Copy").performScrollTo().assertIsEnabled()
        rule.onNodeWithContentDescription("More dice").performScrollTo().performClick()
        rule.onNodeWithText("Ready to roll").assertExists()
        rule.onNodeWithText("Copy").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Roll").performScrollTo().performClick()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Ready to roll").assertExists()
    }

    @Test fun countIsBoundedAtSix() {
        rule.setContent { MaterialTheme { DiceRollScreen {} } }
        repeat(5) { rule.onNodeWithContentDescription("More dice").performClick() }
        rule.onNodeWithContentDescription("More dice").assertIsNotEnabled()
        rule.onNodeWithContentDescription("6 dice selected").assertExists()
    }
}
