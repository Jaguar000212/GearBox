package com.jaguar.gearbox.ui.tools

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.*
import java.text.NumberFormat

@Composable
fun LightMeterScreen(onNavigateBack: () -> Unit, sensorState: LightMeterState = rememberLightMeterState()) {
    val formatter = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 340 }
    ToolScaffold(stringResource(R.string.light_title), Tools.byRoute(Tools.ROUTE_LIGHT_METER)!!.icon, onNavigateBack) {
        AdaptiveToolLayout(input = {
            Text(stringResource(when(sensorState.status) {
                LightMeterStatus.STOPPED -> R.string.light_stopped
                LightMeterStatus.UNAVAILABLE -> R.string.light_unavailable
                LightMeterStatus.REGISTRATION_FAILED -> R.string.light_failed
                LightMeterStatus.WAITING -> R.string.light_waiting
                LightMeterStatus.READING -> R.string.light_reading
            }),style=MaterialTheme.typography.titleMedium)
            // No live-region announcement: rapid readings remain focusable on demand.
            if(sensorState.status == LightMeterStatus.READING) sensorState.lux?.takeIf(::validLightLux)?.let {
                Text(stringResource(R.string.light_lux,formatter.format(it.toString().toBigDecimal())),style=MaterialTheme.typography.displaySmall)
            }
            Text(stringResource(R.string.light_accuracy,stringResource(when(sensorState.accuracy) {
                LightMeterAccuracy.UNKNOWN -> R.string.light_unknown
                LightMeterAccuracy.UNRELIABLE -> R.string.light_unreliable
                LightMeterAccuracy.LOW -> R.string.light_low
                LightMeterAccuracy.MEDIUM -> R.string.light_medium
                LightMeterAccuracy.HIGH -> R.string.light_high
            })))
        },result = {
            sensorState.maximumRange?.takeIf { it.isFinite() && it > 0 }?.let {
                Text(stringResource(R.string.light_range,formatter.format(it.toString().toBigDecimal())))
            }
            sensorState.resolution?.takeIf { it.isFinite() && it > 0 }?.let {
                Text(stringResource(R.string.light_resolution,formatter.format(it.toString().toBigDecimal())))
            }
            Text(stringResource(R.string.light_warning),style=MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.light_local),style=MaterialTheme.typography.bodySmall)
        })
    }
}
