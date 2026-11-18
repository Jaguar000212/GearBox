package com.jaguar.gearbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.BubbleLevelScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test
class AngleFinderUiTest {
    @get:Rule val rule=createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    @Test fun temporaryZeroResetsOnRotationAndUnreliableState() {
        val state=mutableStateOf(OrientationState(OrientationSnapshot(0f,0f,0f),OrientationStatus.READY,
            DisplayGravity(0.0,1.0,1.0),0))
        rule.setContent { isolated.Provide { MaterialTheme { BubbleLevelScreen({},state.value) } } }
        rule.onNodeWithText("Set relative zero").performScrollTo().performClick()
        rule.onNodeWithText("zero reference: 45.0",substring=true).assertExists()
        // Simulate stop/restart coalescing: Compose sees READY both times, but a new continuity.
        rule.runOnIdle {state.value=state.value.copy(continuity=1)}
        rule.onNodeWithText("zero reference: none",substring=true).assertExists()
        rule.onNodeWithText("Set relative zero").performScrollTo().performClick()
        rule.runOnIdle {state.value=state.value.copy(displayRotation=1)}
        rule.onNodeWithText("zero reference: none",substring=true).assertExists()
        rule.onNodeWithText("Set relative zero").performScrollTo().performClick()
        rule.runOnIdle {state.value=OrientationState(status=OrientationStatus.UNRELIABLE)}
        rule.onNodeWithText("Clear relative zero").assertIsNotEnabled()
    }
}
