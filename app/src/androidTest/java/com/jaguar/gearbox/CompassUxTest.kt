package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jaguar.gearbox.logic.OrientationSnapshot
import com.jaguar.gearbox.ui.components.OrientationState
import com.jaguar.gearbox.ui.components.OrientationStatus
import com.jaguar.gearbox.ui.tools.CompassScreen
import org.junit.Rule
import org.junit.Test

class CompassUxTest {
    @get:Rule val rule = createComposeRule()
    @Test fun magneticHeadingHasAccuracyGuidance() {
        rule.setContent { MaterialTheme { CompassScreen({}, OrientationState(OrientationSnapshot(0f, 0f, 0f), OrientationStatus.READY)) } }
        rule.onNodeWithText("Magnetic heading").assertExists()
        rule.onNodeWithText("Hold the device flat", substring = true).assertExists()
        rule.onNodeWithText("0°", substring = true).assertExists()
    }
}
