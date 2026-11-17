package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.jaguar.gearbox.logic.OrientationSnapshot
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.BubbleLevelScreen
import com.jaguar.gearbox.ui.tools.CompassScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SelectorSensorAccessibilityTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test fun selectorReturnsKeyboardFocusAfterCloseSelectionAndBack() {
        lateinit var inputMode: InputModeManager
        compose.setContent { isolated.Provide { MaterialTheme {
            inputMode = LocalInputModeManager.current
            var choice by remember { mutableStateOf("First") }
            CompactChoiceSelector("Mode", choice, listOf("First", "Second"), { it }, { choice = it })
        } } }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        fun click(text: String) = compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick) { it() }
        click("Mode: First")
        click("Close")
        compose.onNodeWithText("Mode: First").assertIsFocused()
        click("Mode: First")
        compose.onNode(hasText("Second") and isSelectable()).performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("Mode: Second").assertIsFocused()
        click("Mode: Second")
        pressBack()
        compose.onNodeWithText("Mode: Second").assertIsFocused()
    }

    @Test fun unreliableLiveLevelIsHiddenButCapturedSnapshotRemainsAvailable() {
        val sensor = mutableStateOf(OrientationState(OrientationSnapshot(0f, 0f, 0f), OrientationStatus.READY))
        compose.setContent { isolated.Provide { MaterialTheme { BubbleLevelScreen({}, sensor.value) } } }
        compose.onNodeWithText("Capture reading").performScrollTo().performClick()
        compose.runOnIdle { sensor.value = sensor.value.copy(status = OrientationStatus.UNRELIABLE) }
        compose.onNodeWithText(isolated.getString(R.string.sensor_live_hidden)).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.sensor_previous_capture)).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.bubble_level_ready)).assertDoesNotExist()
        compose.onNodeWithText("Capture reading").assertIsNotEnabled()
        compose.onNodeWithText("Dismiss snapshot").performScrollTo().assertExists()
        compose.onNodeWithText(isolated.getString(R.string.sensor_unreliable))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test fun compassUnavailableTransitionDoesNotDiscardCapturedSnapshot() {
        val sensor = mutableStateOf(OrientationState(OrientationSnapshot(0f, 0f, 0f), OrientationStatus.READY))
        compose.setContent { isolated.Provide { MaterialTheme { CompassScreen({}, sensor.value) } } }
        compose.onNodeWithText("Capture reading").performScrollTo().performClick()
        compose.runOnIdle { sensor.value = OrientationState(status = OrientationStatus.FAILED) }
        compose.onNodeWithText(isolated.getString(R.string.sensor_live_hidden)).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.sensor_previous_capture)).assertExists()
        compose.onNodeWithText("Capture reading").assertIsNotEnabled()
        compose.onNodeWithText("Dismiss snapshot").performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.sensor_previous_capture)).assertDoesNotExist()
    }
}
