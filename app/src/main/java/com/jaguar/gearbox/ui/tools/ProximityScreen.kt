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
fun ProximityScreen(onNavigateBack: () -> Unit, state: ProximityState = rememberProximityState()) {
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 340 }
    ToolScaffold(stringResource(R.string.proximity_title), Tools.byRoute(Tools.ROUTE_PROXIMITY)!!.icon, onNavigateBack) {
        AdaptiveToolLayout(input = {
            Text(stringResource(when(state.status) {
                ProximityStatus.STOPPED -> R.string.proximity_stopped
                ProximityStatus.UNAVAILABLE -> R.string.proximity_unavailable
                ProximityStatus.REGISTRATION_FAILED -> R.string.proximity_failed
                ProximityStatus.WAITING -> R.string.proximity_waiting
                ProximityStatus.READING -> R.string.proximity_reading
            }))
            if(state.status==ProximityStatus.READING) state.distanceCm?.takeIf(::validProximityDistance)?.let {
                Text(stringResource(when(state.classification) {
                    ProximityClassification.UNKNOWN -> R.string.proximity_unknown
                    ProximityClassification.NEAR -> R.string.proximity_near
                    ProximityClassification.FAR -> R.string.proximity_far
                }),style=MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.proximity_distance,format.format(it.toString().toBigDecimal())))
            }
            Text(stringResource(R.string.proximity_accuracy,stringResource(when(state.accuracy) {
                ProximityAccuracy.UNKNOWN -> R.string.light_unknown
                ProximityAccuracy.UNRELIABLE -> R.string.light_unreliable
                ProximityAccuracy.LOW -> R.string.light_low
                ProximityAccuracy.MEDIUM -> R.string.light_medium
                ProximityAccuracy.HIGH -> R.string.light_high
            })))
        }, result = {
            state.maximumRange?.let { Text(stringResource(R.string.proximity_maximum,format.format(it.toString().toBigDecimal()))) }
            state.resolution?.let { Text(stringResource(R.string.proximity_resolution,format.format(it.toString().toBigDecimal()))) }
            Text(stringResource(R.string.proximity_warning))
            Text(stringResource(R.string.proximity_local))
        })
    }
}
