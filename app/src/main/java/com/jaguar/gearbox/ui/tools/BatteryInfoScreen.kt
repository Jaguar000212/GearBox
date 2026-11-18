package com.jaguar.gearbox.ui.tools

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.*
import java.text.NumberFormat

@Composable
fun BatteryInfoScreen(onNavigateBack: () -> Unit, state: BatteryInfoState = rememberBatteryInfoState()) {
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 3 }
    val unknown = stringResource(R.string.battery_unknown)
    ToolScaffold(stringResource(R.string.battery_title), Tools.byRoute(Tools.ROUTE_BATTERY_INFO)!!.icon, onNavigateBack) {
        AdaptiveToolLayout(input = {
            Text(stringResource(when(state.status) {
                BatteryInfoStatus.STOPPED -> R.string.battery_stopped
                BatteryInfoStatus.WAITING -> R.string.battery_waiting
                BatteryInfoStatus.UNAVAILABLE -> R.string.battery_unavailable
                BatteryInfoStatus.AVAILABLE -> R.string.battery_reading
            }))
            val battery = state.snapshot?.let(::sanitizeBatteryPresence)
            if (battery?.present == false) Text(stringResource(R.string.battery_absent))
            else if (battery != null) {
                if (battery.present == null) Text(stringResource(R.string.battery_presence_unknown))
                Text(stringResource(R.string.battery_percent, battery.percent?.let(format::format) ?: unknown))
                Text(stringResource(R.string.battery_temperature, battery.temperatureCelsius?.let(format::format) ?: unknown))
                Text(stringResource(R.string.battery_voltage, battery.voltageVolts?.let(format::format) ?: unknown))
                Text(stringResource(R.string.battery_current, battery.currentMilliamps?.let(format::format) ?: unknown))
                Text(stringResource(R.string.battery_charge, battery.chargeMilliampHours?.let(format::format) ?: unknown))
                Text(stringResource(R.string.battery_status, stringArrayResource(R.array.battery_statuses)[battery.status.ordinal]))
                Text(stringResource(R.string.battery_power, stringArrayResource(R.array.battery_powers)[battery.power.ordinal]))
                Text(stringResource(R.string.battery_health, stringArrayResource(R.array.battery_healths)[battery.health.ordinal]))
            }
        }, result = {
            Text(stringResource(R.string.battery_warning))
            Text(stringResource(R.string.battery_local))
        })
    }
}
