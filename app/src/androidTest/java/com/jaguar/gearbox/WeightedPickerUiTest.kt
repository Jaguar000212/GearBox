package com.jaguar.gearbox
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.tools.TeamPickerScreen
import org.junit.Assert.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
class WeightedPickerUiTest {
    @get:Rule val rule=createComposeRule()
    private val isolated=IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    @Test fun removalKeepsRemainingWeightAndOwnerAligned() {
        val context=isolated
        val memory=ToolMemoryStore(context)
        memory.saveSettings(memory.settings().copy(rememberGameSessions=true,rememberModes=false,historyEnabled=false,rememberResults=false))
        val prefs=context.getSharedPreferences("gearbox_prefs",Context.MODE_PRIVATE)
        prefs.edit().putString("team_picker.names","A\nB").putString("team_picker.weights","2\n7")
            .putString("team_picker.weight_names","A\nB").commit()
        rule.setContent {isolated.Provide {MaterialTheme {TeamPickerScreen {}}}}
        rule.onNodeWithText("Weighted pick").performScrollTo().performClick()
        rule.onNodeWithText("Edit",substring=true).performScrollTo().performClick()
        rule.onNodeWithContentDescription("Remove participant 1: A").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals("B",prefs.getString("team_picker.names",null))
            assertEquals("7",prefs.getString("team_picker.weights",null))
            assertEquals("B",prefs.getString("team_picker.weight_names",null))
        }
    }
}
