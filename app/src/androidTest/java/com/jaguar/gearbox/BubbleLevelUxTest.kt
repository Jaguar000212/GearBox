package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jaguar.gearbox.logic.OrientationSnapshot
import com.jaguar.gearbox.ui.components.OrientationState
import com.jaguar.gearbox.ui.components.OrientationStatus
import com.jaguar.gearbox.ui.tools.BubbleLevelScreen
import org.junit.Rule
import org.junit.Test

class BubbleLevelUxTest {
    @get:Rule val rule = createComposeRule()
    @Test fun levelExplainsToleranceAndLimitations() {
        rule.setContent { MaterialTheme { BubbleLevelScreen({}, OrientationState(OrientationSnapshot(0f, 0f, 0f), OrientationStatus.READY)) } }
        rule.onNodeWithText("Level means pitch and roll are both less than 1° from zero.").assertExists()
        rule.onNodeWithText("Place the device flat", substring = true).assertExists()
    }
}
