package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.TipCalculatorScreen
import org.junit.Rule
import org.junit.Test

class TipCalculatorRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun calculationAndClearStayConsistent() {
        rule.setContent { MaterialTheme { TipCalculatorScreen {} } }
        rule.onNodeWithText("Bill amount").performTextInput("100")
        rule.onNodeWithText("115.00", substring = false).assertExists()
        rule.onNodeWithText("Clear", substring = true).performScrollTo().performClick()
        rule.onNodeWithText("115.00", substring = false).assertDoesNotExist()
    }
}
