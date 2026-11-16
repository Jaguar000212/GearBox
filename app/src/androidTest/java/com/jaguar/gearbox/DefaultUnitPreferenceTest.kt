package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.ui.tools.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DefaultUnitPreferenceTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    private fun imperial() { assertTrue(UiPreferencesStore(isolated).setDefaultUnitSystem(DefaultUnitSystem.IMPERIAL)) }

    @Test fun freshBmiUsesImperial() {
        imperial()
        compose.setContent { isolated.Provide { MaterialTheme { BmiCalculatorScreen({}) } } }
        compose.onNodeWithText("Height (ft)").assertExists()
        compose.onNodeWithText("Height (cm)").assertDoesNotExist()
    }
    @Test fun freshConverterUsesInchesAndFeet() {
        imperial()
        compose.setContent { isolated.Provide { MaterialTheme { UnitConverterScreen({}) } } }
        compose.onNodeWithText("Inches (in)").assertExists()
        compose.onNodeWithText("Feet (ft)").assertExists()
    }
    @Test fun rulerDefaultDoesNotRemoveCalibrationWarning() {
        imperial()
        compose.setContent { isolated.Provide { MaterialTheme { ScreenRulerScreen({}) } } }
        compose.onNodeWithText(isolated.getString(com.jaguar.gearbox.R.string.ruler_inches)).assertIsSelected()
        compose.onNodeWithText(isolated.getString(com.jaguar.gearbox.R.string.ruler_estimate_guidance)).assertExists()
    }
    @Test fun rememberedBmiModeWinsOverGlobalDefault() {
        imperial()
        val memory = ToolMemoryStore(isolated)
        memory.saveSettings(ToolMemorySettings(rememberModes = true))
        memory.saveMode(Tools.ROUTE_BMI, mapOf("unitSystem" to "METRIC"))
        compose.setContent { isolated.Provide { MaterialTheme { BmiCalculatorScreen({}) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Height (cm)").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Height (ft)").assertDoesNotExist()
    }
    @Test fun rememberedConverterResultWinsOverGlobalDefault() {
        imperial()
        val memory = ToolMemoryStore(isolated)
        memory.saveSettings(ToolMemorySettings(rememberResults = true))
        memory.saveResult(Tools.ROUTE_UNIT_CONVERTER,
            mapOf("category" to "TEMPERATURE", "fromUnit" to "Celsius", "toUnit" to "Fahrenheit", "input" to "0"), "0 °C = 32 °F")
        compose.setContent { isolated.Provide { MaterialTheme { UnitConverterScreen({}) } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("0 °C = 32 °F").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun preferenceChangesNeverOverwriteActiveOrRecreatedDraft() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { isolated.Provide { MaterialTheme { UnitConverterScreen({}) } } }
        compose.onNodeWithText("Value").performTextReplacement("7")
        compose.runOnIdle { imperial() }
        compose.onNodeWithText("7 mm = 0.7 cm").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("7 mm = 0.7 cm").assertExists()
    }
    @Test fun storePersistsUnitChoiceWithoutOverwritingOtherPreferences() {
        val first = UiPreferencesStore(isolated)
        first.setReduceMotion(true)
        first.setShowActiveClockShortcut(false)
        imperial()
        val second = UiPreferencesStore(isolated)
        assertEquals(DefaultUnitSystem.IMPERIAL, second.preferences.value.defaultUnitSystem)
        assertTrue(second.preferences.value.reduceMotion)
        assertFalse(second.preferences.value.showActiveClockShortcut)
        assertTrue(java.io.File(isolated.noBackupFilesDir, "ui_preferences_v1.json").readText().contains("IMPERIAL"))
        second.setReduceMotion(false)
        assertEquals(DefaultUnitSystem.IMPERIAL, first.preferences.value.defaultUnitSystem)
    }
    @Test fun settingsSelectorSavesExplicitChoice() {
        compose.setContent { isolated.Provide { MaterialTheme { com.jaguar.gearbox.ui.screens.DefaultUnitPreference() } } }
        compose.onNodeWithText("Default unit system: Metric").performClick()
        compose.onNodeWithText("Imperial").performClick()
        compose.waitUntil(5_000) {
            UiPreferencesStore(isolated).preferences.value.defaultUnitSystem == DefaultUnitSystem.IMPERIAL
        }
        compose.onNodeWithText("Default unit system: Imperial").assertExists()
    }

}
