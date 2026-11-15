package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jaguar.gearbox.ui.tools.NumberToWordsScreen
import org.junit.Rule
import org.junit.Test

class NumberWordsScreenTest {
    @get:Rule val rule = createComposeRule()
    @Test fun supportsSignAndSharedActions() {
        rule.setContent { MaterialTheme { NumberToWordsScreen {} } }
        rule.onNodeWithText("Whole number").performTextInput("12")
        rule.onNodeWithText("Twelve").assertExists()
        rule.onNodeWithText("Share").assertExists()
        rule.onNodeWithText("Change sign (±)").performClick()
        rule.onNodeWithText("Negative twelve", ignoreCase = true).assertExists()
        rule.onNodeWithText("Clear", substring = true).performClick()
        rule.onNodeWithText("Share").assertDoesNotExist()
    }
}
