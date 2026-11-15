package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.ColorPickerScreen
import org.junit.Rule
import org.junit.Test

class ColorPickerUxTest {
    @get:Rule val rule = createComposeRule()

    @Test fun emptyDraftDoesNotImmediatelyRepopulate() {
        rule.setContent { MaterialTheme { ColorPickerScreen({}) } }
        rule.onNodeWithTag("colorHexInput").performScrollTo().performTextClearance()
        rule.onNodeWithTag("colorHexInput").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        rule.onNodeWithTag("colorHexInput").performTextInput("#FF0000")
        rule.onNodeWithContentDescription("Red").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        rule.onNodeWithTag("colorHexInput").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("#000000")))
    }

    @Test fun partialDraftKeepsLastValidPreview() {
        rule.setContent { MaterialTheme { ColorPickerScreen({}) } }
        rule.onNodeWithTag("colorHexInput").performScrollTo().performTextReplacement("#")
        rule.onNodeWithTag("colorHexInput").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("#")))
        rule.onNodeWithText("Copy HEX").assertExists()
        rule.onNodeWithText("Share").assertExists()
    }
}
