package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jaguar.gearbox.ui.tools.BaseConverterScreen
import org.junit.Rule
import org.junit.Test

class BaseConverterScreenTest {
    @get:Rule val rule = createComposeRule()
    @Test fun convertsAndClearsWithoutChangingBase() {
        rule.setContent { MaterialTheme { BaseConverterScreen {} } }
        rule.onNodeWithText("Value").performTextInput("10")
        rule.onNodeWithText("Binary (base 2)\n1010", substring = true).assertExists()
        rule.onNodeWithText("Share").assertExists()
        rule.onNodeWithText("Clear", substring = true).performClick()
        rule.onNodeWithText("Converted values").assertDoesNotExist()
        rule.onNodeWithText("Decimal (base 10)").assertExists()
    }
    @Test fun malformedDigitsAndSignsShowDigitGuidanceOnlyAfterBlur() {
        assertBlurValidation("12A", "Use digits 0–9 with an optional leading sign.")
    }

    @Test fun doubledSignsAreMalformedNotOverflow() {
        assertBlurValidation("-+1", "Use digits 0–9 with an optional leading sign.")
    }

    @Test fun misplacedSignIsMalformedNotOverflow() {
        assertBlurValidation("1-2", "Use digits 0–9 with an optional leading sign.")
    }

    @Test fun overflowHasDistinctMessageAfterBlur() {
        assertBlurValidation("9223372036854775808", "Outside the signed 64-bit whole-number range.")
    }

    private fun assertBlurValidation(input: String, message: String) {
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            MaterialTheme { BaseConverterScreen {} }
        }
        rule.onNodeWithText("Value").performTextInput(input)
        rule.onNodeWithText(message).assertDoesNotExist()
        rule.runOnIdle { focusManager.clearFocus() }
        rule.onNodeWithText(message).assertExists()
        rule.onNodeWithText("Converted values").assertDoesNotExist()
    }
}
