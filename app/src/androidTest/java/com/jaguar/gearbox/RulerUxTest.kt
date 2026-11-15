package com.jaguar.gearbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jaguar.gearbox.ui.tools.ScreenRulerScreen
import org.junit.Rule
import org.junit.Test
class RulerUxTest {
    @get:Rule val compose = createComposeRule()
    @Test fun unitsExplainTheirSubdivisions() {
        compose.setContent { MaterialTheme { ScreenRulerScreen {} } }
        compose.onNodeWithText("Centimetres · each small division is 1 mm").assertExists()
        compose.onNodeWithText("in").performClick()
        compose.onNodeWithText("Inches · each small division is 1/16 inch").assertExists()
    }
}
