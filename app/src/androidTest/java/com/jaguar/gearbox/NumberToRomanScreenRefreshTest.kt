package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import com.jaguar.gearbox.ui.tools.NumberToRomanScreen
import org.junit.Rule
import org.junit.Test

class NumberToRomanScreenRefreshTest {
    @get:Rule val rule = createComposeRule()
    @Test fun resultAndClear() {
        rule.setContent { MaterialTheme { NumberToRomanScreen {} } }
        rule.onNodeWithText("Number (1-3999)").performTextInput("236")
        rule.onNodeWithText("CCXXXVI").assertExists()
        rule.onNodeWithText(" Share").assertExists()
        rule.onNodeWithText(" Clear").performClick()
        rule.onNodeWithText("CCXXXVI").assertDoesNotExist()
    }
}
