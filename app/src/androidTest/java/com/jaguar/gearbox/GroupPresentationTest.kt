package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.TeamPickerScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GroupPresentationTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    @Test fun hiddenPagesHaveNoTextSemanticsAndNavigationAlwaysHides() {
        compose.setContent { isolated.Provide { MaterialTheme {
            GroupPresentation(listOf("Team 1: Alice", "Team 2: Bob"), {})
        } } }
        compose.onNodeWithText("Team 1: Alice", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Team 2: Bob", useUnmergedTree = true).assertDoesNotExist()
        click("Reveal result")
        compose.onNodeWithText("Team 1: Alice").assertExists()
        click("Next result")
        compose.onNodeWithText("Team 1: Alice", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Team 2: Bob", useUnmergedTree = true).assertDoesNotExist()
        click("Reveal result")
        compose.onNodeWithText("Team 2: Bob").assertExists()
        click("Hide result")
        compose.onNodeWithText("Team 2: Bob", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun recreatedPresentationDoesNotRestoreRevealedContent() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { isolated.Provide { MaterialTheme { GroupPresentation(listOf("Private participant"), {}) } } }
        click("Reveal result")
        compose.onNodeWithText("Private participant").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Private participant", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Result hidden").assertExists()
    }

    @Test fun teamPickerCloseKeepsExistingWinnerAndReopenStartsHidden() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_TEAM_PICKER,
            mapOf("participants" to "Alice", "mode" to "pick", "teams" to "2")))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { TeamPickerScreen({}) }
        } } }
        compose.waitUntil(5_000) { pending.value == null }
        click(isolated.getString(R.string.game_pick_one_222))
        val picked = isolated.getString(R.string.game_picked_1_s, "Alice")
        compose.onNodeWithText(picked).assertExists()
        click("Present existing result")
        compose.onNodeWithText(picked, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.game_participants_1_s_edit, 1), useUnmergedTree = true).assertDoesNotExist()
        click("Reveal result")
        compose.onNodeWithText(picked).assertExists()
        compose.onNodeWithText("Close presentation").performClick()
        compose.onNodeWithText(picked).assertExists()
        click("Present existing result")
        compose.onNodeWithText(picked, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Result hidden").assertExists()
    }
    @Test fun teamPickerRecreationStaysInHiddenPresentation() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_TEAM_PICKER,
            mapOf("participants" to "Alice", "mode" to "pick", "teams" to "2")))
        val restoration = StateRestorationTester(compose)
        restoration.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { TeamPickerScreen({}) }
        } } }
        compose.waitUntil(5_000) { pending.value == null }
        click(isolated.getString(R.string.game_pick_one_222))
        val picked = isolated.getString(R.string.game_picked_1_s, "Alice")
        click("Present existing result")
        click("Reveal result")
        compose.onNodeWithText(picked).assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Result hidden").assertExists()
        compose.onNodeWithText("Close presentation").assertExists()
        compose.onNodeWithText(picked, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.game_participants_1_s_edit, 1), useUnmergedTree = true).assertDoesNotExist()
        click("Reveal result")
        compose.onNodeWithText(picked).assertExists()
    }

}
