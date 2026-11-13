package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.jaguar.gearbox.ui.tools.PasswordGeneratorScreen
import org.junit.Rule
import org.junit.Test

class PasswordStateRestorationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun changingControlsAndRestoringDoesNotRerateExistingPassword() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { MaterialTheme { PasswordGeneratorScreen {} } }
        rule.onNodeWithText("Generate", substring = true).performScrollTo().performClick()
        rule.onNodeWithText("Very strong").assertExists()
        // Disable the three initially enabled classes without generating a new password.
        repeat(3) { index ->
            rule.onAllNodes(isToggleable())[index].performScrollTo().performClick()
        }
        rule.onNodeWithText("Very strong").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Very strong").assertExists()
    }
}
