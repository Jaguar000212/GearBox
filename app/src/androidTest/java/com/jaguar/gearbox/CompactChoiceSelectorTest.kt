package com.jaguar.gearbox

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CompactChoiceSelectorTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun selectedValueStaysVisibleAndSelectionDismissesDialog() {
        var picked = "Circle"
        compose.setContent {
            var selected by remember { mutableStateOf("Circle") }
            MaterialTheme {
                CompactChoiceSelector(label = "Shape", selected = selected,
                    choices = listOf("Circle", "Rectangle", "Triangle"), choiceLabel = { it },
                    onSelect = { selected = it; picked = it })
            }
        }
        compose.onNodeWithText("Shape: Circle").performClick()
        compose.onNode(hasText("Circle") and isSelectable()).assertIsSelected()
        compose.onNodeWithText("Triangle").performClick()
        compose.onNodeWithText("Shape: Triangle").assertExists()
        compose.onNodeWithText("Close").assertDoesNotExist()
        compose.runOnIdle { assertEquals("Triangle", picked) }
    }

    @Test fun closingWithoutSelectionDoesNotChangeValue() {
        var changes = 0
        compose.setContent {
            MaterialTheme {
                CompactChoiceSelector(label = "Unit", selected = "Days",
                    choices = listOf("Days", "Weeks", "Months", "Years"), choiceLabel = { it },
                    onSelect = { changes++ })
            }
        }
        compose.onNodeWithText("Unit: Days").performClick()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Unit: Days").assertExists()
        compose.runOnIdle { assertEquals(0, changes) }
    }
}
