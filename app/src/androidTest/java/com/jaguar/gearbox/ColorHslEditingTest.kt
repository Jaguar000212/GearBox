package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.jaguar.gearbox.ui.components.LocalToolImport
import com.jaguar.gearbox.ui.components.LocalConsumeToolImport
import com.jaguar.gearbox.ui.components.ToolImport
import com.jaguar.gearbox.data.Tools
import org.junit.After
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.ui.tools.ColorPickerScreen
import org.junit.Rule
import org.junit.Test

class ColorHslEditingTest {
    @get:Rule val compose = createComposeRule()
    private val context = IsolatedToolTestContext()
    @After fun cleanup() { context.cleanup() }

    @Test fun hslAppliesOnlyOnRequestAndInvalidInputKeepsAppliedColor() {
        compose.setContent { context.Provide { MaterialTheme { ColorPickerScreen({}) } } }
        compose.onNode(hasText("HSL") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("colorHueInput").performScrollTo().performTextReplacement("120")
        compose.onNodeWithTag("colorSaturationInput").performScrollTo().performTextReplacement("100")
        compose.onNodeWithTag("colorLightnessInput").performScrollTo().performTextReplacement("50")
        compose.onNode(hasText("HEX") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("colorHexInput").assertTextContains("#6750A4")
        compose.onNode(hasText("HSL") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("colorHueInput").assertTextContains("120")
        compose.onNodeWithText("Apply HSL").performScrollTo().performClick()
        compose.onNode(hasText("HEX") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("colorHexInput").assertTextContains("#00FF00")
        compose.onNode(hasText("HSL") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("colorHueInput").performScrollTo().performTextReplacement("361")
        compose.onNodeWithText("Apply HSL").performScrollTo().performClick()
        compose.onNodeWithText("Enter finite numbers: hue 0–360, saturation and lightness 0–100. Use a dot for decimals. The applied color is unchanged.").assertExists()
        compose.onNode(hasText("HEX") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("colorHexInput").assertTextContains("#00FF00")
    }
    @Test fun explicitSameColorImportClearsInvalidDraft() {
        val pending = mutableStateOf<ToolImport?>(null)
        compose.setContent { context.Provide { MaterialTheme {
            CompositionLocalProvider(LocalToolImport provides pending.value,
                LocalConsumeToolImport provides { pending.value = null }) {
                ColorPickerScreen({})
            }
        } } }
        compose.onNode(hasText("HSL") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithTag("colorHueInput").performScrollTo().performTextReplacement("361")
        compose.onNodeWithText("Apply HSL").performScrollTo().performClick()
        compose.runOnIdle { pending.value = ToolImport(Tools.ROUTE_COLOR_PICKER, mapOf("hex" to "#6750A4")) }
        compose.onNodeWithTag("colorHueInput").assertTextContains("256")
        compose.onNodeWithText("Enter finite numbers: hue 0–360, saturation and lightness 0–100. Use a dot for decimals. The applied color is unchanged.").assertDoesNotExist()
    }

}
