package com.jaguar.gearbox

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jaguar.gearbox.ui.components.DecimalField
import org.junit.Rule
import org.junit.Test

class DecimalFieldTest {
    @get:Rule val rule = createComposeRule()

    @Test fun errorsWaitForActualFocusThenBlur() {
        rule.setContent {
            MaterialTheme {
                val focus = LocalFocusManager.current
                var value by remember { mutableStateOf("") }
                Column {
                    DecimalField(value, { value = it }, "Value")
                    Button(onClick = { focus.clearFocus() }) { Text("Finish editing") }
                }
            }
        }
        rule.onNodeWithText("Enter a finite number.").assertDoesNotExist()
        rule.onNodeWithText("Value").performClick().performTextInput("NaN")
        rule.onNodeWithText("Enter a finite number.").assertDoesNotExist()
        rule.onNodeWithText("Finish editing").performClick()
        rule.onNodeWithText("Enter a finite number.").assertExists()
    }
}
