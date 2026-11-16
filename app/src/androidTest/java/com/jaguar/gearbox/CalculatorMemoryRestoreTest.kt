package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.ToolMemorySettings
import com.jaguar.gearbox.data.ToolMemoryStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.tools.PercentageCalculatorScreen
import com.jaguar.gearbox.ui.tools.UnitConverterScreen
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Rule
import org.junit.Test

class CalculatorMemoryRestoreTest {
    @get:Rule val compose = createComposeRule()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(base.cacheDir, "memory-restore-${UUID.randomUUID()}")
    private val isolated = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir(): File = directory.also { it.mkdirs() }
    }
    @After fun cleanup() { directory.deleteRecursively() }

    @Test fun restoringDifferentPercentageModeKeepsBothInputs() {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(rememberResults = true))
        store.saveResult(Tools.ROUTE_PERCENTAGE,
            mapOf("mode" to "WHAT_PERCENT", "field1" to "30", "field2" to "150"), "20%")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated) {
                MaterialTheme { PercentageCalculatorScreen({}) }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("What percentage?").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("30") and hasSetTextAction()).assertExists()
        compose.onNode(hasText("150") and hasSetTextAction()).assertExists()
    }

    @Test fun restoringDifferentUnitCategoryKeepsInputAndSelectedUnits() {
        val store = ToolMemoryStore(isolated)
        store.saveSettings(ToolMemorySettings(rememberResults = true))
        store.saveResult(Tools.ROUTE_UNIT_CONVERTER,
            mapOf("category" to "TEMPERATURE", "fromUnit" to "Fahrenheit", "toUnit" to "Celsius", "input" to "32"), "32 °F = 0 °C")
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated) {
                MaterialTheme { UnitConverterScreen({}) }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Temperature").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("32") and hasSetTextAction()).assertExists()
        compose.onNodeWithText("Fahrenheit (°F)").assertExists()
        compose.onNodeWithText("Celsius (°C)").assertExists()
    }
}
