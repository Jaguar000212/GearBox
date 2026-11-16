package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
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
}
