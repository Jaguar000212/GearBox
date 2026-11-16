package com.jaguar.gearbox

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.logic.PercentageOperation
import com.jaguar.gearbox.ui.components.ShowWorking
import com.jaguar.gearbox.ui.tools.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class CalculatorWorkingTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    private val context = isolated.createConfigurationContext(Configuration(isolated.resources.configuration).apply { setLocale(Locale.ENGLISH) })
    @After fun cleanup() { isolated.cleanup() }

    @Test fun workingUsesActualFormulasAndHandlesUndefinedInputs() {
        assertEquals(listOf("Convert the percentage: 20 ÷ 100 = 0.2.\nMultiply by the value: 0.2 × 150 = 30."),
            percentageWorking(context, PercentageOperation.OF_VALUE, 20.0, 150.0))
        assertTrue(percentageWorking(context, PercentageOperation.WHAT_PERCENT, 20.0, 0.0).isEmpty())
        assertTrue(percentageWorking(context, PercentageOperation.CHANGE, -100.0, -50.0).first().contains("-50%"))
        assertEquals(listOf("Tip = 100 × (15 ÷ 100) = 15.", "Total = 100 + 15 = 115.", "Per person = 115 ÷ 2 = 57.5."),
            tipWorking(context, 100.0, 15.0, 2))
        assertTrue(tipWorking(context, 100.0, 15.0, 0).isEmpty())
        val discounts = discountWorking(context, 100.0, listOf(20.0, 10.0))
        assertEquals("Discount 1: 100 × (1 − 20 ÷ 100) = 80.", discounts[0])
        assertEquals("Discount 2: 80 × (1 − 10 ÷ 100) = 72.", discounts[1])
        assertTrue(discountWorking(context, 0.0, listOf(20.0)).last().contains("rather than dividing by zero"))
        assertEquals("Base = 118 ÷ (1 + 18 ÷ 100) = 100.", taxWorking(context, 118.0, 18.0, true)[0])
        assertEquals("Tax = 100 × (18 ÷ 100) = 18.", taxWorking(context, 100.0, 18.0, false)[0])
        assertTrue(taxWorking(context, -1.0, 18.0, true).isEmpty())
        assertTrue(ratioWorking(context, 0.0, 0.0).isEmpty())
        assertTrue(ratioWorking(context, 12.0, 18.0)[2].endsWith("= 2 : 3."))
        assertTrue(ratioWorking(context, -0.5, 0.25)[2].endsWith("= -2 : 1."))
    }

    @Test fun workingStartsCollapsedAndExpandedTextTracksCurrentInputs() {
        val steps = mutableStateOf(listOf("First calculation"))
        compose.setContent { isolated.Provide { MaterialTheme { ShowWorking(steps.value) } } }
        compose.onNodeWithText("First calculation").assertDoesNotExist()
        compose.onNodeWithText("Show working").performClick()
        compose.onNodeWithText("First calculation").assertExists()
        compose.runOnIdle { steps.value = listOf("Updated calculation") }
        compose.onNodeWithText("First calculation").assertDoesNotExist()
        compose.onNodeWithText("Updated calculation").assertExists()
        compose.onNodeWithText("Hide working").performClick()
        compose.onNodeWithText("Updated calculation").assertDoesNotExist()
    }
}
