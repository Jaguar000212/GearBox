package com.jaguar.gearbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.tools.LightMeterScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.text.NumberFormat

class LightMeterScreenTest {
    @get:Rule val compose = createComposeRule()
    private val isolated = IsolatedToolTestContext()
    @After fun cleanup() { isolated.cleanup() }
    @Test fun missingHardwareHasNoReadingAndNoCameraFallback() {
        compose.setContent { isolated.Provide { MaterialTheme {
            LightMeterScreen({},LightMeterState(status=LightMeterStatus.UNAVAILABLE))
        } } }
        compose.onNodeWithText(isolated.getString(R.string.light_unavailable)).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.light_reading)).assertDoesNotExist()
    }
    @Test fun liveReadingIsRemovedWhenStoppedAndAccuracyIsExplicit() {
        val state=mutableStateOf(LightMeterState(LightMeterStatus.READING,125f,LightMeterAccuracy.LOW,10000f,1f))
        compose.setContent { isolated.Provide { MaterialTheme { LightMeterScreen({},state.value) } } }
        val reading=isolated.getString(R.string.light_lux,NumberFormat.getNumberInstance().format(125))
        compose.onNodeWithText(reading).assertExists()
        compose.onNodeWithText(isolated.getString(R.string.light_accuracy,isolated.getString(R.string.light_low))).assertExists()
        compose.runOnIdle { state.value=LightMeterState(status=LightMeterStatus.STOPPED) }
        compose.onNodeWithText(reading).assertDoesNotExist()
        compose.onNodeWithText(isolated.getString(R.string.light_stopped)).assertExists()
    }
}
