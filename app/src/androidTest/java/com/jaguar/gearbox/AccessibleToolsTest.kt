package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.ui.tools.ColorPickerScreen
import com.jaguar.gearbox.ui.tools.TicTacToeScreen
import org.junit.Rule
import org.junit.Test

class AccessibleToolsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun allNineCellsHavePositionsAndWinningCellsAreDisabled() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("gearbox_prefs", 0)
        val originalWins = prefs.getInt("tic_tac_toe.x_wins", 0)
        try {
            rule.setContent { MaterialTheme { TicTacToeScreen {} } }
            repeat(9) { index ->
                rule.onNodeWithTag("tic_cell_$index").assertExists()
            }
            rule.onNodeWithTag("tic_cell_8").performScrollTo().assertIsDisplayed()
            rule.onNodeWithContentDescription("Row 3, column 3").assertExists()
            listOf(0, 3, 1, 4, 2).forEach { index ->
                rule.onNodeWithTag("tic_cell_$index").performScrollTo().performClick()
            }
            rule.onNodeWithText("X wins!").assertExists()
            rule.onNodeWithTag("tic_cell_0")
                .assertIsNotEnabled()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "X, winning cell"))
            rule.onNodeWithTag("tic_cell_8").assertIsNotEnabled()
        } finally {
            prefs.edit().putInt("tic_tac_toe.x_wins", originalWins).commit()
        }
    }

    @Test fun savedSwatchHasHexNameAndSlidersHaveChannelNames() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("gearbox_prefs", 0)
        val originalPalette = prefs.getString("color_picker.recent", null)
        prefs.edit().remove("color_picker.recent").commit()
        try {
            rule.setContent { MaterialTheme { ColorPickerScreen {} } }
            rule.onNodeWithContentDescription("Save color #6750A4").performClick()
            rule.onNodeWithContentDescription("Saved color #6750A4").assertExists()
            rule.onNodeWithContentDescription("Red").assertExists()
            rule.onNodeWithContentDescription("Green").assertExists()
            rule.onNodeWithContentDescription("Blue").assertExists()
        } finally {
            prefs.edit().apply {
                if (originalPalette == null) remove("color_picker.recent")
                else putString("color_picker.recent", originalPalette)
            }.commit()
        }
    }
}
