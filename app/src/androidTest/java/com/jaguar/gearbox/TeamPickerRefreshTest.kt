package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.ui.tools.TeamPickerScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TeamPickerRefreshTest {
    @get:Rule val rule = createComposeRule()
    @Before fun resetNames() {
        SimplePrefsStore(ApplicationProvider.getApplicationContext<Context>()).putString("team_picker.names", "")
    }
    private fun add(name: String) {
        rule.onNodeWithText("Add a name").performScrollTo().performTextInput(name)
        rule.onNodeWithContentDescription("Add name").performClick()
    }
    @Test fun rosterChangesInvalidateResultsAndClampTeamCount() {
        rule.setContent { MaterialTheme { TeamPickerScreen {} } }
        add("A"); add("B"); add("C")
        rule.onNodeWithText("Pick one").performScrollTo().performClick()
        rule.onNodeWithText("Picked:", substring = true).assertExists()
        add("D")
        rule.onNodeWithText("Picked:", substring = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("More teams").performScrollTo().performClick()
        rule.onNodeWithContentDescription("More teams").performClick()
        rule.onNodeWithText("Split into teams").performScrollTo().performClick()
        rule.onNodeWithText("Team 4:", substring = true).assertExists()
        rule.onNodeWithContentDescription("Remove participant 4: D").performScrollTo().performClick()
        rule.onNodeWithText("Team 4:", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Split into teams").performScrollTo().assertIsEnabled()
        rule.onNodeWithContentDescription("More teams").assertIsNotEnabled()
    }
    @Test fun oneNameCanBePickedAndClearRequiresConfirmation() {
        rule.setContent { MaterialTheme { TeamPickerScreen {} } }
        add("Solo")
        rule.onNodeWithText("Pick one").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Split into teams").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Participants (1)").assertExists()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Clear names").performClick()
        rule.onNodeWithText("Participants (1)").assertDoesNotExist()
    }
}
