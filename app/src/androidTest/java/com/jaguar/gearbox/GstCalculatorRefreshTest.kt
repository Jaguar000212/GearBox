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
}
