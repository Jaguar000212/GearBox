package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.ui.tools.TambolaScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TambolaRefreshTest {
    @get:Rule val rule = createComposeRule()
    private val store get() = SimplePrefsStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Before fun before() = reset()
    @After fun after() = reset()
    private fun reset() { store.putString("tambola.called", ""); store.putInt("tambola.last_called", 0) }

    @Test fun finalNumberCompletesGameAndResetNeedsConfirmation() {
        store.putString("tambola.called", (1..89).joinToString(","))
        store.putInt("tambola.last_called", 89)
        rule.setContent { MaterialTheme { TambolaScreen {} } }
        rule.onNodeWithText("Call number").performClick()
        rule.onNodeWithText("All 90 numbers called. Game complete!").assertExists()
        rule.onNodeWithText("Game complete").assertIsNotEnabled()
        rule.onNodeWithText("Reset game").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("All 90 numbers called. Game complete!").assertExists()
        rule.onNodeWithText("Reset game").performClick()
        rule.onNodeWithText("Reset").performClick()
        rule.onNodeWithText("Ready to play").assertExists()
        rule.onNodeWithText("Reset game").assertIsNotEnabled()
    }

    @Test fun boardExposesCalledStateAndLastNumberIsReachable() {
        store.putString("tambola.called", "31,47")
        store.putInt("tambola.last_called", 47)
        rule.setContent { MaterialTheme { TambolaScreen {} } }
        rule.onNodeWithContentDescription("31, called").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("47, current number, called").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("90, not called").performScrollTo().assertIsDisplayed()
    }
}
