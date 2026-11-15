package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.OneOfTwoScreen
import org.junit.Rule
import org.junit.Test

class OneOfTwoRefreshTest {
    @get:Rule val rule = createComposeRule()

    @Test fun editingInvalidatesResultAndClearResetsInputs() {
        rule.setContent { MaterialTheme { OneOfTwoScreen {} } }
        rule.onNodeWithText("Pick one").assertIsNotEnabled()
        rule.onNodeWithText("Option 1").performTextInput("Tea")
        rule.onNodeWithText("Option 2").performTextInput("Coffee")
        rule.onNodeWithText("Pick one").performScrollTo().performClick()
        rule.onNodeWithText("Copy").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Option 1").performScrollTo().performTextReplacement("Water")
        rule.onNodeWithText("Copy").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Pick one").performScrollTo().assertIsNotEnabled()
    }

    @Test fun duplicateLabelsIdentifyFirstSlot() = assertDuplicateSlot(0)

    @Test fun duplicateLabelsIdentifySecondSlot() = assertDuplicateSlot(1)

    private fun assertDuplicateSlot(index: Int) {
        rule.setContent { MaterialTheme { OneOfTwoScreen(chooseIndex = { index }) {} } }
        rule.onNodeWithText("Option 1").performTextInput("Same")
        rule.onNodeWithText("Option 2").performTextInput("Same")
        rule.onNodeWithText("Pick one").performScrollTo().performClick()
        rule.onNodeWithText("Option ${index + 1} selected\nSame").assertExists()
    }
}
