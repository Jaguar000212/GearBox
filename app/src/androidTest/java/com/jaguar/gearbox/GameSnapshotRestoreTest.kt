package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.LocalToolImport
import com.jaguar.gearbox.ui.components.LocalConsumeToolImport
import com.jaguar.gearbox.ui.components.ToolImport
import com.jaguar.gearbox.ui.tools.TambolaScreen
import com.jaguar.gearbox.ui.tools.TicTacToeScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class GameSnapshotRestoreTest {
    @get:Rule val compose = createComposeRule()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefix = "game_snapshot_${UUID.randomUUID()}_"
    private val directory = File(base.cacheDir, prefix)
    private val isolated = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir(): File = directory.also { it.mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            base.getSharedPreferences(prefix + name, mode)
    }
    @After fun cleanup() {
        directory.deleteRecursively()
        base.deleteSharedPreferences(prefix + "gearbox_prefs")
    }

    private fun openStoredSnapshot() {
        compose.onNodeWithContentDescription("History").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Restore").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Restore").performClick()
    }

    @Test fun tambolaRequiresConfirmationBeforeReplacingProgress() {
        val prefs = isolated.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("tambola.called", "1,2").putInt("tambola.last_called", 2).commit()
        val memory = ToolMemoryStore(isolated)
        memory.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true))
        memory.record(Tools.ROUTE_TAMBOLA, mapOf("calls" to "3,4"), "Two saved calls")
        memory.saveResult(Tools.ROUTE_TAMBOLA, mapOf("calls" to "3,4"), "Two saved calls")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated) { MaterialTheme { TambolaScreen({}) } }
        }
        openStoredSnapshot()
        compose.onNodeWithText("Restore Tambola snapshot?").assertExists()
        compose.onNodeWithText("Keep current game").performClick()
        compose.onNodeWithContentDescription("2, current number, called").assertExists()
        assertEquals("1,2", prefs.getString("tambola.called", ""))
        openStoredSnapshot()
        compose.onNodeWithText("Restore").performClick()
        compose.onNodeWithContentDescription("4, current number, called").assertExists()
        assertEquals("3,4", prefs.getString("tambola.called", ""))
    }

    @Test fun ticTacToeRestoresBoardWithoutPlayingMoveOrChangingTally() {
        val prefs = isolated.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("tic_tac_toe.x_wins", 3).putInt("tic_tac_toe.o_wins", 4)
            .putInt("tic_tac_toe.draws", 5).commit()
        val memory = ToolMemoryStore(isolated)
        memory.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true))
        memory.record(Tools.ROUTE_TIC_TAC_TOE, mapOf("board" to "XO..X...."), "O to play")
        memory.saveResult(Tools.ROUTE_TIC_TAC_TOE, mapOf("board" to "XXXOO...."), "X wins")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated) { MaterialTheme { TicTacToeScreen({}) } }
        }
        compose.onNodeWithTag("tic_cell_0").performClick()
        openStoredSnapshot()
        compose.onNodeWithText("Restore game snapshot?").assertExists()
        compose.onNodeWithText("Restore").performClick()
        compose.onNodeWithTag("tic_cell_4").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "X"))
        compose.onNodeWithTag("tic_cell_1").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "O"))
        assertEquals(3, prefs.getInt("tic_tac_toe.x_wins", -1))
        assertEquals(4, prefs.getInt("tic_tac_toe.o_wins", -1))
        assertEquals(5, prefs.getInt("tic_tac_toe.draws", -1))
    }
    @Test fun deletedCaptureIsNotRecordedAgainAfterRecreation() {
        val prefs = isolated.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("tambola.called", "1,2").putInt("tambola.last_called", 2).commit()
        val memory = ToolMemoryStore(isolated)
        memory.saveSettings(ToolMemorySettings(historyEnabled = true))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalContext provides isolated) { MaterialTheme { TambolaScreen({}) } }
        }
        compose.onNodeWithText("Capture snapshot").performScrollTo().performClick()
        compose.waitUntil(5_000) { memory.history(Tools.ROUTE_TAMBOLA).size == 1 }
        memory.clearHistory(Tools.ROUTE_TAMBOLA)
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertTrue(memory.history(Tools.ROUTE_TAMBOLA).isEmpty())
    }

    @Test fun workbenchImportAlsoConfirmsBeforeReplacingPersistedTambolaGame() {
        val prefs = isolated.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("tambola.called", "1,2").putInt("tambola.last_called", 2).commit()
        var consumed = false
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides isolated,
                LocalToolImport provides ToolImport(Tools.ROUTE_TAMBOLA, mapOf("calls" to "3,4")),
                LocalConsumeToolImport provides { consumed = true },
            ) { MaterialTheme { TambolaScreen({}) } }
        }
        compose.onNodeWithText("Restore Tambola snapshot?").assertExists()
        assertTrue(consumed)
        compose.onNodeWithText("Keep current game").performClick()
        assertEquals("1,2", prefs.getString("tambola.called", ""))
        compose.onNodeWithContentDescription("2, current number, called").assertExists()
    }

    @Test fun clearingImmediatelyAfterCaptureRejectsNotYetStartedRecording() {
        val prefs = isolated.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("tambola.called", "1,2").putInt("tambola.last_called", 2).commit()
        val memory = ToolMemoryStore(isolated)
        memory.saveSettings(ToolMemorySettings(historyEnabled = true))
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated) { MaterialTheme { TambolaScreen({}) } }
        }
        val capture = compose.onNodeWithText("Capture snapshot").performScrollTo()
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle {
            capture()
            // Same UI turn: clear happens before the capture's recording effect can start.
            memory.clearHistory(Tools.ROUTE_TAMBOLA)
        }
        compose.onNodeWithContentDescription("History").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("No saved runs yet.", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(memory.history(Tools.ROUTE_TAMBOLA).isEmpty())
    }

}
