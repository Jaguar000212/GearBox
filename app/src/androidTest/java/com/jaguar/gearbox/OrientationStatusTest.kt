package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jaguar.gearbox.ui.components.OrientationState
import com.jaguar.gearbox.ui.components.OrientationStatus
import com.jaguar.gearbox.ui.tools.CompassScreen
import org.junit.Rule
import org.junit.Test

class OrientationStatusTest {
    @get:Rule val rule = createComposeRule()

    @Test fun missingSensorIsNotAnIndefiniteLoadingMessage() {
        rule.setContent {
            MaterialTheme {
                CompassScreen({}, OrientationState(status = OrientationStatus.UNAVAILABLE))
            }
        }
        rule.onNodeWithText("This device does not provide the orientation sensor this tool needs.").assertExists()
        rule.onNodeWithText("Waiting for an orientation reading…").assertDoesNotExist()
    }
}
