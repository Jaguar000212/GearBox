package com.jaguar.gearbox

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.components.ToolFocusView
import org.junit.Rule
import org.junit.Test

class ToolFocusViewTest {
    @get:Rule val compose = createComposeRule()

    @Test fun focusActionsUpdateSharedStateAndExitDoesNotResetIt() {
        compose.setContent {
            var count by remember { mutableStateOf(0) }
            var focus by remember { mutableStateOf(true) }
            MaterialTheme {
                Text("Outside count: $count")
                if (focus) ToolFocusView("Counter", onExit = { focus = false }) {
                    Text("Focus count: $count")
                    Button(onClick = { count++ }) { Text("Increase") }
                    Button(onClick = { count-- }) { Text("Decrease") }
                }
            }
        }
        compose.onNodeWithText("Increase").performClick()
        compose.onNodeWithText("Focus count: 1").assertIsDisplayed()
        compose.onNodeWithText("Decrease").performClick()
        compose.onNodeWithText("Focus count: 0").assertIsDisplayed()
        compose.onNodeWithText("Increase").performClick()
        compose.onNodeWithText("Exit focus view").performClick()
        compose.onNodeWithText("Outside count: 1").assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
    }

    @Test fun exitStaysVisibleAboveLongScrollableContent() {
        compose.setContent { MaterialTheme {
            ToolFocusView("Scoreboard", onExit = {}) {
                repeat(40) { Text("Player $it") }
            }
        } }
        compose.onNodeWithText("Player 39").performScrollTo()
        compose.onNodeWithText("Exit focus view").assertIsDisplayed()
    }
}
