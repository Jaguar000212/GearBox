package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
    @Test fun clearInvalidInputsPreservesSelectedModeAndCanCalculateAgain() {
        rule.setContent { isolated.Provide { MaterialTheme { PercentageCalculatorScreen {} } } }
        rule.onNodeWithText("Clear").assertIsNotEnabled()
        rule.onNodeWithText("Calculation: Percentage of a value").performClick()
        rule.onNodeWithText("What percentage?").performClick()
        rule.onNodeWithText("Part", substring = false).performTextInput("5")
        rule.onNodeWithText("Total", substring = false).performTextInput("0")
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Calculation: What percentage?").assertExists()
        rule.onNode(hasText("Part") and hasSetTextAction()).assert(hasText("5").not())
        rule.onNode(hasText("Total") and hasSetTextAction()).assert(hasText("0").not())
        rule.onNodeWithText("Clear").assertIsNotEnabled()
        rule.onNodeWithText("Copy").assertDoesNotExist()
        rule.onNodeWithText("Part", substring = false).performScrollTo().performTextInput("30")
        rule.onNodeWithText("Total", substring = false).performScrollTo().performTextInput("150")
        rule.onNodeWithText("30 out of 150 = 20%").assertExists()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("30 out of 150 = 20%").assertDoesNotExist()
        rule.onNodeWithText("Calculation: What percentage?").assertExists()
    }

}
