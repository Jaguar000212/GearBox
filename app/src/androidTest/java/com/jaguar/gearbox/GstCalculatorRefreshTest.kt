package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.GstCalculatorScreen
import org.junit.Rule
import org.junit.Test

class GstCalculatorRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun calculationAndClearStayConsistent() {
        rule.setContent { MaterialTheme { GstCalculatorScreen {} } }
        rule.onNodeWithText("Amount (before tax)").performTextInput("100")
        rule.onNodeWithText("118", substring = false).assertExists()
        rule.onNodeWithText("Clear", substring = true).performScrollTo().performClick()
        rule.onNodeWithText("118", substring = false).assertDoesNotExist()
    }
    @Test fun invalidInputAndOverflowHaveDistinctRecoveryMessages() {
        rule.setContent { MaterialTheme { GstCalculatorScreen {} } }
        for (value in listOf("NaN", "Infinity", "invalid", "-1")) {
            rule.onNodeWithText("Amount (before tax)").performTextReplacement(value)
            rule.onNodeWithText("Enter a valid, finite, non-negative amount.").assertExists()
        }
        rule.onNodeWithText("Amount (before tax)").performTextReplacement("100")
        for (value in listOf("NaN", "Infinity", "invalid", "-1")) {
            rule.onNodeWithText("Tax rate (%)").performTextReplacement(value)
            rule.onNodeWithText("Enter a valid, finite, non-negative rate.").assertExists()
        }
        rule.onNodeWithText("Tax rate (%)").performTextReplacement("100")
        rule.onNodeWithText("Amount (before tax)").performTextReplacement("1e308")
        rule.onNodeWithText("The result exceeds the supported numeric range. Reduce the amount or tax rate.").assertExists()
        rule.onNodeWithText("Amount (before tax)").performTextReplacement("100")
        rule.onNodeWithText("200", substring = false).assertExists()
    }
}
