package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.jaguar.gearbox.ui.tools.DateCalculatorScreen
import org.junit.Rule
import org.junit.Test

class DateCalculatorPresentationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun validDateOffersConsistentActionsButIncompleteInputDoesNot() {
        rule.setContent { MaterialTheme { DateCalculatorScreen {} } }
        rule.onNodeWithText("Resulting date").assertExists()
        rule.onNodeWithText("Share", substring = true).assertExists()
        rule.onNodeWithText("Number of days").performTextReplacement("")
        rule.onNodeWithText("Resulting date").assertDoesNotExist()
        rule.onNodeWithText("Share", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Number of days").performTextReplacement("1")
        rule.onNodeWithText("Resulting date").assertExists()
    }
}
