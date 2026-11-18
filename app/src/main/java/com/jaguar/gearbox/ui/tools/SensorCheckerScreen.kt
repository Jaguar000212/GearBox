package com.jaguar.gearbox.ui.tools

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.*
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.*
import java.text.NumberFormat

@Composable fun SensorCheckerScreen(onNavigateBack:()->Unit,handle:SensorCheckerHandle=rememberSensorChecker()) {
    var selected by remember{mutableStateOf(CheckedSensor.ACCELEROMETER)}
    val names=stringArrayResource(R.array.checker_names);val units=stringArrayResource(R.array.checker_units)
    val statuses=stringArrayResource(R.array.checker_statuses);val accuracies=stringArrayResource(R.array.checker_accuracies)
    val unknown=stringResource(R.string.checker_unknown);val format=NumberFormat.getNumberInstance().apply{maximumFractionDigits=4}
    ToolScaffold(stringResource(R.string.checker_title),Tools.byRoute(Tools.ROUTE_SENSOR_CHECKER)!!.icon,onNavigateBack){
        AdaptiveToolLayout(input={
            CompactChoiceSelector(stringResource(R.string.checker_select),selected,CheckedSensor.entries,{names[it.ordinal]},{if(it!=selected){handle.stop();selected=it}})
            val meta=handle.inventory[selected]
            Text(stringResource(if(meta?.available==true)R.string.checker_available else R.string.checker_unavailable))
            Text(stringResource(R.string.checker_metadata,meta?.maximumRange?.let(format::format)?:unknown,meta?.resolution?.let(format::format)?:unknown,units[selected.ordinal]))
            Button(onClick={handle.start(selected)},enabled=meta?.available==true){Text(stringResource(R.string.checker_start))}
            OutlinedButton(onClick=handle.stop){Text(stringResource(R.string.checker_stop))}
            Text(statuses[handle.state.status.ordinal])
        },result={
            Text(stringResource(R.string.checker_accuracy,accuracies[handle.state.accuracy.ordinal]))
            if(handle.state.status==SensorCheckStatus.READING){
                val values=checkedSensorValues(selected,handle.state.values)
                values?.forEachIndexed{index,value->Text(stringResource(R.string.checker_value,
                    if(selected.components==1)stringResource(R.string.checker_scalar) else stringArrayResource(R.array.checker_axes)[index],format.format(value),units[selected.ordinal]))}
            }
            Text(stringResource(if(selected==CheckedSensor.ROTATION_VECTOR)R.string.checker_rotation else R.string.checker_axes_note))
            Text(stringResource(R.string.checker_warning));Text(stringResource(R.string.checker_privacy))
        })
    }
}
