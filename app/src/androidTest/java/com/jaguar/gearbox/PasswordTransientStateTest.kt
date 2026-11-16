package com.jaguar.gearbox

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jaguar.gearbox.ui.tools.PasswordGeneratorScreen
import org.junit.Rule
import org.junit.Test

class PasswordTransientStateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun generatedSecretIsNotSavedInInstanceState() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { PasswordGeneratorScreen(onNavigateBack = {}) }
        compose.onNodeWithText("  Generate").performClick()
        compose.onNodeWithText(" Copy").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(" Copy").assertDoesNotExist()
    }
}
