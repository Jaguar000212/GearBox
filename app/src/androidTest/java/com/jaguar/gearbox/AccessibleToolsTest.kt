package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.UUID
import org.junit.After
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
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefix = "accessible_tools_${UUID.randomUUID()}_"
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

    @Test fun allNineCellsHavePositionsAndWinningCellsAreDisabled() {
        rule.setContent {
            CompositionLocalProvider(LocalContext provides isolated) { MaterialTheme { TicTacToeScreen {} } }
        }
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
    }

    @Test fun savedSwatchHasHexNameAndSlidersHaveChannelNames() {
        rule.setContent {
            CompositionLocalProvider(LocalContext provides isolated) { MaterialTheme { ColorPickerScreen {} } }
        }
        rule.onNodeWithContentDescription("Save color #6750A4").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Saved color #6750A4").assertExists()
        rule.onNodeWithContentDescription("Red").assertDoesNotExist()
        rule.onNodeWithText("RGB").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Red").assertExists()
        rule.onNodeWithContentDescription("Green").assertExists()
        rule.onNodeWithContentDescription("Blue").assertExists()
    }
}
