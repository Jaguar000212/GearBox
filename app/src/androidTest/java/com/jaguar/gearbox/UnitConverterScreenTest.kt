package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.UnitConverterScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class UnitConverterScreenTest {
    @get:Rule val rule = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    private fun showConverter() {
        rule.setContent { isolated.Provide { MaterialTheme { UnitConverterScreen {} } } }
    }

    @Test fun defaultResultAndSwapUseUnitSymbols() {
        showConverter()
        rule.onNodeWithText("1 mm = 0.1 cm").assertExists()
        rule.onNodeWithText(" Swap units").performScrollTo().performClick()
        rule.onNodeWithText("1 cm = 10 mm").assertExists()
    }

    @Test fun negativeTemperatureCanBeEnteredWithoutMinusKeyboard() {
        showConverter()
        rule.onNodeWithText("Category: Length").performClick()
        rule.onNodeWithText("Temperature").performClick()
        rule.onNodeWithText("Value").performTextReplacement("40")
        rule.onNodeWithContentDescription("Change sign").performClick()
        rule.onNodeWithText("-40 °C = -40 °F").assertExists()
    }

    @Test fun invalidInputCannotBeCopiedOrShared() {
        showConverter()
        rule.onNodeWithText("Value").performTextReplacement("NaN")
        rule.onNodeWithText("Enter a valid finite number.").assertExists()
        rule.onNodeWithText("Copy").assertIsNotEnabled()
        rule.onNodeWithText("Share").assertIsNotEnabled()
    }
    @Test fun clearPreservesCategoryAndSwappedUnitsForNextConversion() {
        showConverter()
        rule.onNodeWithText("Category: Length").performClick()
        rule.onNodeWithText("Temperature").performClick()
        rule.onNodeWithText(" Swap units").performScrollTo().performClick()
        rule.onNodeWithText("Value").performScrollTo().performTextReplacement("32")
        rule.onNodeWithText("32 °F = 0 °C").assertExists()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Category: Temperature").assertExists()
        rule.onNodeWithText("Fahrenheit (°F)").assertExists()
        rule.onNodeWithText("Celsius (°C)").assertExists()
        rule.onNodeWithText("32 °F = 0 °C").assertDoesNotExist()
        rule.onNodeWithText("Clear").assertIsNotEnabled()
        rule.onNodeWithText("Copy").assertIsNotEnabled()
        rule.onNodeWithText("Share").assertIsNotEnabled()
        rule.onNodeWithText("Value").performScrollTo().performTextReplacement("212")
        rule.onNodeWithText("212 °F = 100 °C").assertExists()
    }

    @Test fun clearAlsoWorksForInvalidConversionInput() {
        showConverter()
        rule.onNodeWithText("Value").performTextReplacement("NaN")
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Enter a value to see the conversion.").assertExists()
        rule.onNodeWithText("Enter a valid finite number.").assertDoesNotExist()
        rule.onNodeWithText("Clear").assertIsNotEnabled()
    }

}
