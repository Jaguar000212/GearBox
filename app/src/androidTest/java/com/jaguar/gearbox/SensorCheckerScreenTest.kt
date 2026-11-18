package com.jaguar.gearbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.SensorCheckerHandle
import com.jaguar.gearbox.ui.tools.SensorCheckerScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class SensorCheckerScreenTest {
    @get:Rule val compose=createComposeRule()
    @Test fun openingDoesNotStartAndExplicitButtonStartsSelectedSensor(){
        var starts=0;var selected:CheckedSensor?=null
        compose.setContent{MaterialTheme{SensorCheckerScreen({},SensorCheckerHandle(SensorCheckState(),
            CheckedSensor.entries.associateWith{CheckedSensorMetadata(true)}, {starts++;selected=it},{}))}}
        compose.runOnIdle{assertEquals(0,starts)}
        compose.onNodeWithText("Start selected sensor").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(1,starts);assertEquals(CheckedSensor.ACCELEROMETER,selected)}
    }
}
