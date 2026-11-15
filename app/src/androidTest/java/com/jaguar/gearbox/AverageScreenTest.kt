package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.AverageCalculatorScreen
import org.junit.Rule
import org.junit.Test

class AverageScreenTest {
    @get:Rule val rule = createComposeRule()
    @Test fun resultsAndActionsFollowCurrentInput() {
        rule.setContent { MaterialTheme { AverageCalculatorScreen {} } }
        rule.onNodeWithText("Copy").assertIsNotEnabled()
        rule.onNodeWithText("Numbers (comma separated)").performTextInput("1,2,3,4,5")
        rule.onNodeWithText("Arithmetic mean").assertExists()
        rule.onNodeWithText("3").assertExists()
        rule.onNodeWithText("Copy").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Copy").assertIsNotEnabled()
        rule.onNodeWithText("Arithmetic mean").assertDoesNotExist()
    }
}
