package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.ui.tools.ChessScoreboardScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChessScoreboardUxTest {
    @get:Rule val rule = createComposeRule()
    @Before fun setup() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("gearbox_prefs", 0).edit()
            .remove("chess_white_name").remove("chess_black_name")
            .remove("chess_white_score").remove("chess_black_score")
            .remove("chess_games_played").commit()
        rule.setContent { MaterialTheme { ChessScoreboardScreen {} } }
    }

    @Test fun bothPlayersAndGamesAreVisible() {
        rule.onNodeWithText("Games").assertIsDisplayed()
        rule.onNodeWithTag("black-score").assertIsDisplayed()
        rule.onNodeWithTag("white-score").assertIsDisplayed()
    }

    @Test fun drawUndoAndResetCancellation() {
        rule.onNodeWithText("Draw").performScrollTo().performClick()
        rule.onAllNodesWithText("0.5").assertCountEquals(2)
        rule.onNodeWithText(" Undo last result").performScrollTo().performClick()
        rule.onNodeWithText(" Undo last result").assertIsNotEnabled()
        rule.onNodeWithText("Draw").performScrollTo().performClick()
        rule.onNodeWithText(" Reset match").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onAllNodesWithText("0.5").assertCountEquals(2)
    }
}
