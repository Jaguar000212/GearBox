package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jaguar.gearbox.ui.tools.PercentageCalculatorScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PercentageScreenTest {
    @get:Rule val rule = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    @Test fun resultIncludesContextAndActionsOnlyWhenValid() {
        rule.setContent { isolated.Provide { MaterialTheme { PercentageCalculatorScreen {} } } }
        rule.onNodeWithText("Copy").assertDoesNotExist()
        rule.onNodeWithText("Percentage", substring = false).performTextInput("15")
        rule.onNodeWithText("Value", substring = false).performTextInput("240")
        rule.onNodeWithText("15% of 240 = 36").assertExists()
        rule.onNodeWithText("Copy").assertExists()
        rule.onNodeWithText("Share").assertExists()
    }

    @Test fun zeroTotalErrorWaitsForBlur() {
        rule.setContent { isolated.Provide { MaterialTheme { PercentageCalculatorScreen {} } } }
        rule.onNodeWithText("Calculation: Percentage of a value").performClick()
        rule.onNodeWithText("What percentage?").performClick()
        rule.onNodeWithText("Total", substring = false).performTextInput("0")
        rule.onNodeWithText("Total must not be zero.").assertDoesNotExist()
        rule.onNodeWithText("Part", substring = false).performClick()
        rule.onNodeWithText("Total must not be zero.").assertExists()
        rule.onNodeWithText("Copy").assertDoesNotExist()
    }
}
