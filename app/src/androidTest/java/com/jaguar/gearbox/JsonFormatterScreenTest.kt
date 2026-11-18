package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.components.*
import com.jaguar.gearbox.ui.tools.JsonFormatterScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class JsonFormatterScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private val inputs = mapOf("source" to " { \"n\": 1.00 } ", "mode" to "MINIFY", "indent" to "2")
    @Test fun explicitImportRequiresRunAndSameInputRestoreInvalidatesOutput() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_JSON_FORMATTER, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { JsonFormatterScreen({}) }
        } } }
        compose.onNodeWithText("{\"n\":1.00}").assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.json_run)).performScrollTo().performClick()
        compose.onNodeWithText("{\"n\":1.00}").assertExists()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_JSON_FORMATTER, inputs) }
        compose.onNodeWithText("{\"n\":1.00}").assertDoesNotExist()
    }
    @Test fun languageSwitchAndHtmlImportInvalidateOutputAndConstrainModes() {
        val pending = mutableStateOf<ToolImport?>(ToolImport(Tools.ROUTE_JSON_FORMATTER, inputs))
        compose.setContent { isolated.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) { JsonFormatterScreen({}) }
        } } }
        fun selector(label: Int, value: String) = isolated.getString(R.string.shared_label_value, isolated.getString(label), value)
        compose.onNodeWithText(isolated.getString(R.string.json_run)).performScrollTo().performClick()
        compose.onNodeWithText("{\"n\":1.00}").assertExists()
        compose.onNodeWithText(selector(R.string.code_language,"JSON")).performScrollTo().performClick()
        compose.onNodeWithText("HTML",useUnmergedTree=true).performClick()
        compose.onNodeWithText("{\"n\":1.00}").assertDoesNotExist()
        compose.onNodeWithText(selector(R.string.json_mode,isolated.getString(R.string.html_normalize))).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.shared_share)).assertIsNotEnabled()
        val html = mapOf("source" to "<p>sample", "mode" to "PRETTY", "indent" to "2", "language" to "HTML")
        compose.runOnIdle { pending.value=ToolImport(Tools.ROUTE_JSON_FORMATTER,html) }
        compose.onNodeWithText(isolated.getString(R.string.shared_share)).assertIsNotEnabled()
        val normalized="<html><head></head><body><p>sample</p></body></html>"
        compose.onNodeWithText(normalized).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.json_run)).performScrollTo().performClick()
        compose.onNodeWithText(normalized).assertExists()
        compose.onNodeWithText(selector(R.string.code_language,"HTML")).performScrollTo().performClick()
        compose.onNodeWithText("XML",useUnmergedTree=true).performClick()
        compose.onNodeWithText(normalized).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.shared_share)).assertIsNotEnabled()
        compose.onNodeWithText(selector(R.string.json_mode,isolated.getString(R.string.xml_attributes))).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.json_validate),useUnmergedTree=true).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.xml_attributes),useUnmergedTree=true).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.json_minify),useUnmergedTree=true).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.html_normalize),useUnmergedTree=true).assertDoesNotExist()
    }
    @Test fun privateDefaultDoesNotSaveAndInvalidInputDoesNotProduceOutput() {
        val store = ToolMemoryStore(isolated)
        assertTrue(store.saveSettings(ToolMemorySettings(historyEnabled = true, rememberResults = true)))
        compose.setContent { isolated.Provide { MaterialTheme { JsonFormatterScreen({}) } } }
        compose.onNodeWithText(isolated.getString(R.string.code_source)).performTextReplacement("{\"secret\":123}")
        compose.onNodeWithText(isolated.getString(R.string.json_run)).performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(1600); compose.waitForIdle(); Thread.sleep(1500)
        assertTrue(store.history(Tools.ROUTE_JSON_FORMATTER).isEmpty())
        assertTrue(store.loadResult(Tools.ROUTE_JSON_FORMATTER) == null)
        compose.onNodeWithText(isolated.getString(R.string.code_source)).performScrollTo().performTextReplacement("{\"a\":1,\"a\":2}")
        compose.onNodeWithText(isolated.getString(R.string.json_run)).performScrollTo().performClick()
        compose.onNodeWithText(isolated.getString(R.string.json_error_duplicate), substring = true).assertExists()
    }
}
