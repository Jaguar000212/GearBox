package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.DiscountCalculatorScreen
import org.junit.Rule
import org.junit.Test

class DiscountCalculatorRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun calculationAndClearStayConsistent() {
        rule.setContent { MaterialTheme { DiscountCalculatorScreen {} } }
        rule.onNodeWithText("Original price").performTextInput("100")
        rule.onNodeWithText("Discount 1 (%)").performTextInput("20")
        rule.onNodeWithText("80", substring = false).assertExists()
        rule.onNodeWithText("Clear", substring = true).performScrollTo().performClick()
        rule.onNodeWithText("80", substring = false).assertDoesNotExist()
    }
}
