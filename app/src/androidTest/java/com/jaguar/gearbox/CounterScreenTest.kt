package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.CounterStore
import com.jaguar.gearbox.ui.tools.CounterScreen
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CounterScreenTest {
    @get:Rule val rule = createComposeRule()
    private val store get() = CounterStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Before fun resetBefore() { store.reset() }
    @After fun resetAfter() { store.reset() }

    @Test fun selectingStepDoesNotCountAndButtonsUseStep() {
        rule.setContent { MaterialTheme { CounterScreen {} } }
        rule.onNodeWithText("5").performClick()
        rule.runOnIdle { assertEquals(0, store.count) }
        rule.onNodeWithContentDescription("Increase by 5").performClick()
        rule.runOnIdle { assertEquals(5, store.count) }
        rule.onNodeWithContentDescription("Decrease by 5").performClick()
        rule.runOnIdle { assertEquals(0, store.count) }
    }

    @Test fun resetRequiresConfirmationAndCancelPreservesCount() {
        store.adjust(22)
        rule.setContent { MaterialTheme { CounterScreen {} } }
        rule.onNodeWithText("  Reset").performClick()
        rule.onNodeWithText("Reset count?").assertExists()
        rule.onNodeWithText("Keep counting").performClick()
        rule.runOnIdle { assertEquals(22, store.count) }
        rule.onNodeWithText("  Reset").performClick()
        rule.onNodeWithText("Reset count").performClick()
        rule.runOnIdle { assertEquals(0, store.count) }
    }
}
