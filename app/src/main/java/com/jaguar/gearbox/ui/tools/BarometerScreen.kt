package com.jaguar.gearbox.ui.tools

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.*
import java.text.NumberFormat

@Composable
fun BarometerScreen(onNavigateBack: () -> Unit, state: BarometerState = rememberBarometerState()) {
    // Reference is transient, not a remembered mode or a location/weather lookup.
    var reference by remember { mutableStateOf("1013.25") }
    val parsed = parseSeaLevelReference(reference)
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }
    ToolScaffold(stringResource(R.string.barometer_title), Tools.byRoute(Tools.ROUTE_BAROMETER)!!.icon, onNavigateBack) {
        AdaptiveToolLayout(input = {
            Text(stringResource(when(state.status) {
                BarometerStatus.STOPPED -> R.string.barometer_stopped
                BarometerStatus.UNAVAILABLE -> R.string.barometer_unavailable
                BarometerStatus.REGISTRATION_FAILED -> R.string.barometer_failed
                BarometerStatus.WAITING -> R.string.barometer_waiting
                BarometerStatus.READING -> R.string.barometer_reading
            }))
            OutlinedTextField(value=reference, onValueChange={ if(it.length<=7) reference=it },
                label={Text(stringResource(R.string.barometer_reference))}, singleLine=true, isError=parsed==null)
            Text(stringResource(R.string.barometer_reference_help))
            if(parsed==null) Text(stringResource(R.string.barometer_reference_error),color=MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.barometer_accuracy,stringResource(when(state.accuracy) {
                BarometerAccuracy.UNKNOWN -> R.string.light_unknown
                BarometerAccuracy.UNRELIABLE -> R.string.light_unreliable
                BarometerAccuracy.LOW -> R.string.light_low
                BarometerAccuracy.MEDIUM -> R.string.light_medium
                BarometerAccuracy.HIGH -> R.string.light_high
            })))
        }, result = {
            if(state.status==BarometerStatus.READING) state.pressureHpa?.takeIf(::validPressureHpa)?.let { pressure ->
                Text(stringResource(R.string.barometer_pressure,format.format(pressure)),style=MaterialTheme.typography.headlineMedium)
                if(parsed!=null) approximatePressureAltitude(pressure.toDouble(),parsed)?.let {
                    Text(stringResource(R.string.barometer_altitude,format.format(it)))
                }
            }
            Text(stringResource(R.string.barometer_warning))
            Text(stringResource(R.string.barometer_local))
        })
    }
}
