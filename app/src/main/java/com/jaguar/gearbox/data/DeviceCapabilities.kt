package com.jaguar.gearbox.data

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager

/** Availability only: this does not request permissions, activate sensors, or open a camera. */
data class DeviceCapabilities(val orientationSensor: Boolean?, val cameraFlash: Boolean?) {
    fun hint(route: String): String? = when (route) {
        Tools.ROUTE_COMPASS, Tools.ROUTE_BUBBLE_LEVEL -> when (orientationSensor) {
            true -> "Uses orientation sensor; accuracy varies"
            false -> "Required orientation sensor unavailable on this device"
            null -> "Requires an orientation sensor; availability could not be checked"
        }
        Tools.ROUTE_FLASHLIGHT -> when (cameraFlash) {
            true -> "Requires camera permission and an available flash"
            false -> "Camera flash unavailable on this device"
            null -> "Requires camera flash; availability could not be checked"
        }
        Tools.ROUTE_SCREEN_RULER -> "Estimated scale; check against a physical ruler"
        else -> null
    }

    companion object {
        fun read(context: Context): DeviceCapabilities {
            return DeviceCapabilities(
                orientationSensor = runCatching {
                    val sensors = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                    sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
                }.getOrNull(),
                cameraFlash = runCatching {
                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
                }.getOrNull(),
            )
        }
    }
}
