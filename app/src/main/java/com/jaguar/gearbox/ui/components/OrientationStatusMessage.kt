package com.jaguar.gearbox.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R

@Composable
fun OrientationStatusMessage(status: OrientationStatus) {
    val resource = when (status) {
        OrientationStatus.WAITING -> R.string.sensor_waiting
        OrientationStatus.UNAVAILABLE -> R.string.sensor_unavailable
        OrientationStatus.FAILED -> R.string.sensor_failed
        OrientationStatus.UNRELIABLE -> R.string.sensor_unreliable
        OrientationStatus.READY -> return
    }
    Text(stringResource(resource), style = MaterialTheme.typography.bodyMedium)
}
