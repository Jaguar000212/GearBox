package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.FuelCostScreen
import org.junit.Rule
import org.junit.Test

class FuelCostRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun calculationAndClearStayConsistent() {
        rule.setContent { MaterialTheme { FuelCostScreen {} } }
        rule.onNodeWithText("Trip distance (km/mi)").performTextInput("300")
        rule.onNodeWithText("Mileage (distance per unit fuel)").performTextInput("15")
        rule.onNodeWithText("Fuel price per unit").performTextInput("100")
        rule.onNodeWithText("2000", substring = false).assertExists()
        rule.onNodeWithText("Clear", substring = true).performScrollTo().performClick()
        rule.onNodeWithText("2000", substring = false).assertDoesNotExist()
    }
}
