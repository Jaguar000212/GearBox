package com.jaguar.gearbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import com.jaguar.gearbox.ui.tools.BmiCalculatorScreen
import org.junit.Rule
import org.junit.Test
class BmiRefreshTest {
    @get:Rule val rule = createComposeRule()
    @Test fun metricResultAndClear() {
        rule.setContent { MaterialTheme { BmiCalculatorScreen {} } }
        rule.onNodeWithText("Height (cm)").performTextInput("175")
        rule.onNodeWithText("Weight (kg)").performTextInput("70")
        rule.onNodeWithText("Body mass index").assertExists()
        rule.onNodeWithText("Copy").assertExists()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Copy").assertDoesNotExist()
    }
    @Test fun imperialWholeFeetStillWorks() {
        rule.setContent { MaterialTheme { BmiCalculatorScreen {} } }
        rule.onNodeWithText("Imperial (ft, in / lbs)").performClick()
        rule.onNodeWithText("Height (ft)").performTextInput("6")
        rule.onNodeWithText("Weight (lbs)").performTextInput("180")
        rule.onNodeWithText("Body mass index").assertExists()
    }
}
