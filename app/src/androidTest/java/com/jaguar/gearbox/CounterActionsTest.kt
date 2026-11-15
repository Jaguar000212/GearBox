package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.CounterStore
import com.jaguar.gearbox.ui.tools.CounterScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CounterActionsTest {
    @get:Rule val rule = createComposeRule()
    private val store get() = CounterStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Before fun prepare() { store.reset() }
    @After fun cleanup() { store.reset() }

    @Test fun actionsAvailableForZeroAndCopyDoesNotMutateCount() {
        rule.setContent { MaterialTheme { CounterScreen {} } }
        rule.onNodeWithText("Copy").performScrollTo().assertIsEnabled().performClick()
        rule.onNodeWithText("Share").assertIsEnabled()
        rule.runOnIdle { assertEquals(0, store.count) }
    }

    @Test fun externalStoreUpdatesRemainVisibleAtIntegerLimit() {
        rule.setContent { MaterialTheme { CounterScreen {} } }
        rule.runOnIdle { store.adjust(Int.MAX_VALUE) }
        rule.onNodeWithText(Int.MAX_VALUE.toString()).assertExists()
        rule.onNodeWithText("Copy").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(Int.MAX_VALUE, store.count) }
    }
}
