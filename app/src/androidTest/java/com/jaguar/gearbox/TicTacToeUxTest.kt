package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.ui.tools.TicTacToeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TicTacToeUxTest {
    @get:Rule val rule = createComposeRule()

    @Test fun unfinishedRoundRequiresConfirmationAndCancelPreservesTurn() {
        rule.setContent { MaterialTheme { TicTacToeScreen {} } }
        rule.onNodeWithTag("tic_cell_0").performScrollTo().performClick()
        rule.onNodeWithTag("tic_new_round").performScrollTo().performClick()
        rule.onNodeWithText("Discard this round?").assertExists()
        rule.onNodeWithText("Keep playing").performClick()
        rule.onNodeWithTag("tic_cell_0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "X"),
        )
        rule.onNodeWithTag("tic_new_round").performScrollTo().performClick()
        rule.onNodeWithTag("tic_confirm_discard").performClick()
        rule.onNodeWithTag("tic_cell_0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Empty"),
        )
        rule.onNodeWithText("X's turn").assertExists()
    }

    @Test fun toolbarBackConfirmsOnlyAnUnfinishedRound() {
        var backCalls = 0
        rule.setContent { MaterialTheme { TicTacToeScreen { backCalls++ } } }
        rule.onNodeWithContentDescription("Back").performClick()
        rule.runOnIdle { assertEquals(1, backCalls) }
        rule.onNodeWithTag("tic_cell_0").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.runOnIdle { assertEquals(1, backCalls) }
        rule.onNodeWithText("Keep playing").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Leave game").performClick()
        rule.runOnIdle { assertEquals(2, backCalls) }
    }

    @Test fun recreationPreservesBoardTurnAndPendingConfirmation() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent { MaterialTheme { TicTacToeScreen {} } }
        rule.onNodeWithTag("tic_cell_4").performScrollTo().performClick()
        rule.onNodeWithTag("tic_new_round").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Keep playing").performClick()
        rule.onNodeWithTag("tic_cell_4").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "X"),
        )
        rule.onNodeWithText("O's turn").assertExists()
    }

    @Test fun completedRoundRestartsWithoutConfirmationOrErasingScore() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("gearbox_prefs", 0)
        val originalWins = prefs.getInt("tic_tac_toe.x_wins", 0)
        try {
            rule.setContent { MaterialTheme { TicTacToeScreen {} } }
            listOf(0, 3, 1, 4, 2).forEach {
                rule.onNodeWithTag("tic_cell_$it").performScrollTo().performClick()
            }
            rule.onNodeWithText("X wins!").assertExists()
            rule.onNodeWithTag("tic_new_round").performScrollTo().performClick()
            rule.onNodeWithText("Discard this round?").assertDoesNotExist()
            rule.onNodeWithText("X's turn").assertExists()
            assertEquals(originalWins + 1, prefs.getInt("tic_tac_toe.x_wins", 0))
        } finally {
            prefs.edit().putInt("tic_tac_toe.x_wins", originalWins).commit()
        }
    }
}
