package com.jaguar.gearbox

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.jaguar.gearbox.ui.components.LocalDateSaver
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class DateStateRestorationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun selectedDateSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            var date by rememberSaveable(stateSaver = LocalDateSaver) {
                mutableStateOf(LocalDate.of(2020, 1, 1))
            }
            Button(onClick = { date = LocalDate.of(2024, 2, 29) }) {
                Text(date.toString())
            }
        }
        rule.onNodeWithText("2020-01-01").performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("2024-02-29").assertIsDisplayed()
    }
}
