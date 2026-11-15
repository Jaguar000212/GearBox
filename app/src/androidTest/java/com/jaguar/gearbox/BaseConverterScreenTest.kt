package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
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
}
