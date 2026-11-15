package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.RandomNumberScreen
import org.junit.Rule
import org.junit.Test

class RandomNumberRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun editedBoundsInvalidateResultAndSharing() {
        rule.setContent { MaterialTheme { RandomNumberScreen {} } }
        rule.onNodeWithText("Minimum").performTextInput("12")
        rule.onNodeWithText("Maximum").performTextInput("12")
        rule.onNodeWithText("Generate").performScrollTo().performClick()
        rule.onNodeWithText("Range: 12 to 12 (inclusive)").assertExists()
        rule.onNodeWithText("Copy").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Maximum").performScrollTo().performTextReplacement("30")
        rule.onNodeWithText("Copy").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Share").assertIsNotEnabled()
    }

    @Test fun invalidBoundsHaveFieldErrorsAndClearResetsForm() {
        rule.setContent { MaterialTheme { RandomNumberScreen {} } }
        rule.onNodeWithText("Minimum").performTextInput("20")
        rule.onNodeWithText("Maximum").performTextInput("10")
        rule.onNodeWithText("Generate").performClick()
        rule.onNodeWithText("Maximum must be at least the minimum.").assertExists()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Maximum must be at least the minimum.").assertDoesNotExist()
        rule.onNodeWithText("Copy").assertIsNotEnabled()
    }
}
