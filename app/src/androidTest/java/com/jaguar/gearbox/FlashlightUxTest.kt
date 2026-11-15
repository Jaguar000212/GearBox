package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.jaguar.gearbox.ui.tools.FlashlightControls
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FlashlightUxTest {
    @get:Rule val compose = createComposeRule()
    @Test fun offControlHasExplicitAction() {
        var clicks = 0
        compose.setContent { MaterialTheme { FlashlightControls(false) { clicks++ } } }
        compose.onNodeWithContentDescription("Turn on flashlight").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
