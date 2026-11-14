package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.LoanCalculatorScreen
import org.junit.Rule
import org.junit.Test

class LoanCalculatorRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun calculationAndClearStayConsistent() {
        rule.setContent { MaterialTheme { LoanCalculatorScreen {} } }
        rule.onNodeWithText("Loan amount").performTextInput("120")
        rule.onNodeWithText("Annual interest rate (%)").performTextInput("12")
        rule.onNodeWithText("Loan term (years)").performTextInput("5")
        rule.onNodeWithText("Interest share of total repayment: 25.07%", substring = true).assertExists()
        rule.onNodeWithText("Clear", substring = true).performScrollTo().performClick()
        rule.onNodeWithText("Interest share of total repayment: 25.07%", substring = true).assertDoesNotExist()
    }
}
