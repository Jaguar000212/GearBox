package com.jaguar.gearbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import com.jaguar.gearbox.ui.tools.RatiosScreen
import org.junit.Rule
import org.junit.Test
class RatiosRefreshTest {
    @get:Rule val rule = createComposeRule()
    @Test fun showsContextAndClearsResult() {
        rule.setContent { MaterialTheme { RatiosScreen {} } }
        rule.onNodeWithText("First value").performTextInput("12")
        rule.onNodeWithText("Second value").performTextInput("18")
        rule.onNodeWithText("Simplified ratio").assertExists()
        rule.onNodeWithText("Copy", substring = true).assertExists()
        rule.onNodeWithText("Clear").performClick()
        rule.onNodeWithText("Copy", substring = true).assertDoesNotExist()
    }

    @Test fun distinguishesMissingAndInvalidValuesAndRecovers() {
        rule.setContent { MaterialTheme { RatiosScreen {} } }
        rule.onNodeWithText("First value").performTextInput("12")
        rule.onNodeWithText("Enter both values to see the simplified ratio.").assertExists()
        for (invalid in listOf("abc", "NaN", "Infinity")) {
            rule.onNodeWithText("Second value").performTextReplacement(invalid)
            rule.onNodeWithText("Enter valid, finite numbers in both fields.").assertExists()
            rule.onNodeWithText("Copy", substring = true).assertDoesNotExist()
        }
        rule.onNodeWithText("Second value").performTextReplacement("18")
        rule.onNodeWithText("Simplified ratio").assertExists()
        rule.onNodeWithText("Copy", substring = true).assertExists()
    }
}
