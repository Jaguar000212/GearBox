package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jaguar.gearbox.ui.tools.GeometryCalculatorScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GeometryRefreshTest {
    @get:Rule val rule = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    @Test fun rectangleShowsUnitMeaningAndClearRemovesResult() {
        rule.setContent { isolated.Provide { MaterialTheme { GeometryCalculatorScreen {} } } }
        rule.onNodeWithText("Shape: Circle").performClick()
        rule.onNodeWithText("Rectangle").performClick()
        rule.onNodeWithText("Width").performTextInput("2")
        rule.onNodeWithText("Height").performTextInput("6")
        rule.onNodeWithText("Area: 12 square units\nPerimeter: 16 units").assertExists()
        rule.onNodeWithText("Copy").assertExists()
        rule.onNodeWithText("Clear").performClick()
        rule.onNodeWithText("Copy").assertDoesNotExist()
    }
}
