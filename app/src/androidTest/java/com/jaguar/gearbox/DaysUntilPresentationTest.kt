package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.ui.tools.DaysUntilScreen
import java.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DaysUntilPresentationTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var store: SimplePrefsStore
    private var oldLabel = ""
    private var oldDate = ""

    @Before fun seedEvent() {
        store = SimplePrefsStore(InstrumentationRegistry.getInstrumentation().targetContext)
        oldLabel = store.getString("days_until.label", "")
        oldDate = store.getString("days_until.target_date", "")
        store.putString("days_until.label", "Test event")
        store.putString("days_until.target_date", LocalDate.now().toString())
    }

    @After fun restoreEvent() {
        store.putString("days_until.label", oldLabel)
        store.putString("days_until.target_date", oldDate)
    }

    @Test fun todayResultAndPickerCancellationPreserveEvent() {
        rule.setContent { MaterialTheme { DaysUntilScreen {} } }
        rule.onNodeWithText("Test event is today!").assertExists()
        rule.onNodeWithText("Target date:", substring = true).performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Test event is today!").assertExists()
        rule.onNodeWithText("Share", substring = true).assertExists()
    }
}
