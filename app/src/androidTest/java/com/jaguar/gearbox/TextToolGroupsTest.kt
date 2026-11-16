package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.TextToolsScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class TextToolGroupsTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    private fun show() {
        compose.setContent { isolated.Provide { MaterialTheme { TextToolsScreen({}) } } }
    }

    @Test fun conversionCanBeChosenBeforeTypingAndClearPreservesChoice() {
        show()
        compose.onNodeWithText("Transform text").assertExists()
        compose.onNodeWithText("Convert to: Title Case").performScrollTo().performClick()
        compose.onNodeWithText("UPPERCASE").performClick()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Hello world")
        compose.onNodeWithText("HELLO WORLD").assertExists()
        compose.onNode(hasSetTextAction()).assertTextContains("Hello world")
        compose.onNodeWithText("About your input").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Words").assertExists()
        compose.onNodeWithText("UTF-16 units").assertExists()
        compose.onNodeWithText("Clear", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Convert to: UPPERCASE").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("About your input").assertDoesNotExist()
        compose.onNodeWithText("HELLO WORLD").assertDoesNotExist()
    }

    @Test fun informationDescribesOriginalNotCamelCaseResult() {
        show()
        compose.onNode(hasSetTextAction()).performTextInput("A man a plan a canal Panama")
        compose.onNodeWithText("Convert to: Title Case").performScrollTo().performClick()
        compose.onNodeWithText("camelCase").performClick()
        compose.onNodeWithText("aManAPlanACanalPanama").assertExists()
        compose.onNodeWithText("Palindrome ✓").performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertTextContains("A man a plan a canal Panama")
    }
}
