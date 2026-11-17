package com.jaguar.gearbox.ui.components

import android.hardware.SensorManager
import org.junit.Assert.*
import org.junit.Test

class OrientationAccuracyTest {
    @Test fun onlyMediumAndHighPermitLiveEstimates() {
        assertFalse(isOrientationAccuracyUsable(SensorManager.SENSOR_STATUS_UNRELIABLE))
        assertFalse(isOrientationAccuracyUsable(SensorManager.SENSOR_STATUS_ACCURACY_LOW))
        assertFalse(isOrientationAccuracyUsable(SensorManager.SENSOR_STATUS_NO_CONTACT))
        assertFalse(isOrientationAccuracyUsable(99))
        assertTrue(isOrientationAccuracyUsable(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
        assertTrue(isOrientationAccuracyUsable(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
    }
}
