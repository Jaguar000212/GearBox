package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.After
import org.junit.Rule
import org.junit.Test

class CalculatorAdaptiveActionsTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    private fun show(route: String, inputs: Map<String, String>, screen: @Composable () -> Unit) {
        val pending = mutableStateOf<ToolImport?>(ToolImport(route, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }, content = screen)
        } } }
        compose.waitUntil(5_000) { pending.value == null }
    }

    @Test fun ageClearRemovesDatesAndResultWithoutOpeningPicker() {
        show(Tools.ROUTE_AGE, mapOf("earlierDate" to "2000-01-01", "laterDate" to "2025-01-01")) { AgeCalculatorScreen({}) }
        compose.onNodeWithText("Age: 25 years, 0 months, 0 days").assertExists()
        compose.onNodeWithText("Clear", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Age: 25 years, 0 months, 0 days").assertDoesNotExist()
        compose.onAllNodesWithText("Choose date", substring = true).assertCountEquals(2)
        compose.onNodeWithText("Clear", substring = false).assertIsNotEnabled()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
    }

    @Test fun daysUntilClearRemovesEventAndTarget() {
        show(Tools.ROUTE_DAYS_UNTIL, mapOf("label" to "Example event", "targetDate" to "2030-01-01")) { DaysUntilScreen({}) }
        compose.onNodeWithText("Clear", substring = false).performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithText("Choose date", substring = true).assertExists()
        compose.onNodeWithText("Clear", substring = false).assertIsNotEnabled()
        compose.onNodeWithText("Copy", substring = false).assertDoesNotExist()
    }

    @Test fun dateClearBlanksAmountButPreservesUnitAndDirection() {
        show(Tools.ROUTE_DATE_CALCULATOR, mapOf("baseDate" to "2025-01-01", "amountInput" to "3", "unit" to "MONTHS", "direction" to "SUBTRACT")) { DateCalculatorScreen({}) }
        compose.onNodeWithText("Resulting date").assertExists()
        compose.onNodeWithText("Clear", substring = false).performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        val today = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        compose.onNodeWithText("Starting date: $today", substring = true).assertExists()
        compose.onNodeWithText("Unit: Months").assertExists()
        compose.onNodeWithText("Subtract").assertIsSelected()
        compose.onNodeWithText("Resulting date").assertDoesNotExist()
        compose.onNodeWithText("Clear", substring = false).assertIsNotEnabled()
    }

    @Test fun gstClearPreservesRateAndIncludedTaxMode() {
        show(Tools.ROUTE_GST, mapOf("amount" to "120", "rate" to "20", "inclusive" to "true")) { GstCalculatorScreen({}) }
        compose.onNodeWithText("Before tax").assertExists()
        compose.onNodeWithText("Clear", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Amount (tax included)").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithText("Tax rate (%)").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("20")))
        compose.onNodeWithText("Remove tax").assertIsSelected()
        compose.onNodeWithText("Before tax").assertDoesNotExist()
        compose.onNodeWithText("Clear", substring = false).assertIsNotEnabled()
    }

    @Test fun geometryClearKeepsShapeAndRemovesResult() {
        show(Tools.ROUTE_GEOMETRY, mapOf("shape" to "RECTANGLE", "field1" to "3", "field2" to "4", "field3" to "")) { GeometryCalculatorScreen({}) }
        compose.onNodeWithText("Rectangle results").assertExists()
        compose.onNodeWithText("Clear", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Shape: Rectangle").assertExists()
        compose.onNodeWithText("Rectangle results").assertDoesNotExist()
        compose.onNodeWithText("Clear", substring = false).assertIsNotEnabled()
    }
}
