package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import com.jaguar.gearbox.ui.tools.DecimalToFractionScreen
import org.junit.Rule
import org.junit.Test

class DecimalToFractionScreenRefreshTest {
    @get:Rule val rule = createComposeRule()
    @Test fun resultAndClear() {
        rule.setContent { MaterialTheme { DecimalToFractionScreen {} } }
        rule.onNodeWithText("Decimal number").performTextInput("12.96")
        rule.onNodeWithText("324/25").assertExists()
        rule.onNodeWithText(" Share").assertExists()
        rule.onNodeWithText(" Clear").performClick()
        rule.onNodeWithText("324/25").assertDoesNotExist()
    }
}
