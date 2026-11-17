package com.jaguar.gearbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.components.ColorInspection
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ColorInspectionTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    @Test fun pairUpdatesOnlyExplicitlyAndRestoresWithoutChangingPicker() {
        val red = mutableIntStateOf(255)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { isolated.Provide { MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) { ColorInspection(red.intValue, 0, 0) }
        } } }
        compose.onNodeWithText("Foreground: #000000").assertExists()
        compose.onNodeWithText("Background: #FFFFFF").assertExists()
        compose.onNodeWithText("Use current as foreground").performScrollTo().performClick()
        compose.onNodeWithText("Foreground: #FF0000").assertExists()
        compose.runOnIdle { red.intValue = 0 }
        compose.onNodeWithText("Foreground: #FF0000").assertExists()
        compose.onNodeWithText("Use current as background").performScrollTo().performClick()
        compose.onNodeWithText("Background: #000000").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Foreground: #FF0000").assertExists()
        compose.onNodeWithText("Background: #000000").assertExists()
        compose.onNodeWithText("Swap foreground and background").performScrollTo().performClick()
        compose.onNodeWithText("Foreground: #000000").assertExists()
        compose.onNodeWithText("Background: #FF0000").assertExists()
        compose.onNodeWithText("Current applied color: #000000").assertExists()
    }
}
