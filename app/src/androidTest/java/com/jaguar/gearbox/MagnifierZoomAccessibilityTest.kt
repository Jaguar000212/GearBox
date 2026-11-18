package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.jaguar.gearbox.ui.tools.MagnifierZoomSlider
import org.junit.After
import org.junit.Rule
import org.junit.Test

class MagnifierZoomAccessibilityTest {
    @get:Rule val rule = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }

    @Test fun zoomControlExposesLocalizedCurrentValueWithoutCamera() {
        val zoom = mutableFloatStateOf(1.5f)
        rule.setContent { isolated.Provide { MaterialTheme {
            MagnifierZoomSlider(zoom.floatValue, 1f, 4f) { zoom.floatValue = it }
        } } }
        fun assertValue(value: Float) {
            val label = isolated.getString(R.string.magnifier_zoom, value)
            rule.onNodeWithContentDescription(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, label))
        }
        assertValue(1.5f)
        rule.runOnIdle { zoom.floatValue = 3f }
        assertValue(3f)
    }
}
