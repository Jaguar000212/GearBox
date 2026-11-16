package com.jaguar.gearbox

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.ui.components.ToolActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ToolActionsLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun narrowLargeTextActionsRemainReachable() {
        var copies = 0
        var shares = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                MaterialTheme { Box(Modifier.width(240.dp)) {
                    ToolActions(onCopy = { copies++ }, onShare = { shares++ }, onClear = {})
                } }
            }
        }
        compose.onNodeWithText("Copy").assertIsDisplayed().performClick()
        compose.onNodeWithText("Share").assertIsDisplayed().performClick()
        compose.onNodeWithText("Clear").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, copies); assertEquals(1, shares) }
    }
}
