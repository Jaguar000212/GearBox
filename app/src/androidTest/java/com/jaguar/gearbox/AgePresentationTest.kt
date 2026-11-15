package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jaguar.gearbox.ui.tools.AgeCalculatorScreen
import org.junit.Rule
import org.junit.Test

class AgePresentationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun emptyStateAndPickerCancellationDoNotInventResult() {
        rule.setContent { MaterialTheme { AgeCalculatorScreen {} } }
        rule.onNodeWithText("Your age difference will appear here once both dates are selected.").assertExists()
        rule.onNodeWithText("Earlier date:", substring = true).performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Copy", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Later date:", substring = true).assertExists()
    }
}
