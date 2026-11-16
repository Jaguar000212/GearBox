package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.jaguar.gearbox.ui.tools.PasswordGeneratorScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PasswordStateRestorationTest {
    @get:Rule val rule = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    @Test fun preciseLengthAndClear() {
        rule.setContent { isolated.Provide { MaterialTheme { PasswordGeneratorScreen {} } } }
        rule.onNodeWithText("16 characters · Customize").performClick()
        rule.onNodeWithText("Longer").performScrollTo().performClick()
        rule.onNodeWithText("17").assertExists()
        rule.onNodeWithText("Shorter").performClick()
        rule.onNodeWithText("16").assertExists()
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Generate", substring = true).performScrollTo().performClick()
        rule.onNodeWithText("Clear password").performScrollTo().performClick()
        rule.onNodeWithText("Estimated strength: Very strong").assertDoesNotExist()
    }

    @Test fun changingControlsDoesNotRerateButRecreationClearsSecretAndStrength() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { isolated.Provide { MaterialTheme { PasswordGeneratorScreen {} } } }
        rule.onNodeWithText("Generate", substring = true).performScrollTo().performClick()
        rule.onNodeWithText("Estimated strength: Very strong").assertExists()
        // Disable the three initially enabled classes without generating a new password.
        rule.onNodeWithText("16 characters · Customize").performScrollTo().performClick()
        listOf("Uppercase (A-Z)", "Lowercase (a-z)", "Numbers (0-9)").forEach { label ->
            rule.onNodeWithText(label).performScrollTo().performClick()
        }
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Estimated strength: Very strong").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Estimated strength:", substring = true).assertDoesNotExist()
        rule.onNodeWithText(" Copy").assertDoesNotExist()
        rule.onNodeWithText("Clear password").assertDoesNotExist()
        rule.onNodeWithText("0 character types enabled").assertExists()
    }
}
