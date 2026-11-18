package com.jaguar.gearbox.ui.tools

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.*
import java.text.NumberFormat

@Composable
fun PitchTunerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val meter = rememberPitchTuner()
    var permissionNotice by remember { mutableStateOf(false) }
    // A permission result never starts capture, including delayed/background grants.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionNotice = true }
    val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    ToolScaffold(stringResource(R.string.pitch_title), Tools.byRoute(Tools.ROUTE_PITCH_TUNER)!!.icon, onNavigateBack) {
        AdaptiveToolLayout(input = {
            Text(stringResource(when (meter.state.status) {
                SoundMeterStatus.STOPPED -> R.string.sound_stopped
                SoundMeterStatus.STARTING -> R.string.sound_starting
                SoundMeterStatus.READING -> R.string.sound_reading
                SoundMeterStatus.BUSY -> R.string.sound_busy
                SoundMeterStatus.ERROR -> R.string.sound_error
                SoundMeterStatus.PERMISSION_REQUIRED -> R.string.sound_permission
            }))
            if (permissionNotice) Text(stringResource(R.string.sound_permission_notice))
            val running = meter.state.status == SoundMeterStatus.STARTING || meter.state.status == SoundMeterStatus.READING
            Button(onClick = {
                permissionNotice = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) meter.start()
                else permission.launch(Manifest.permission.RECORD_AUDIO)
            }, enabled = !running) { Text(stringResource(R.string.sound_start)) }
            Button(onClick = meter.stop, enabled = running) { Text(stringResource(R.string.sound_stop)) }
        }, result = {
            val estimate = meter.state.estimate
            if (estimate != null) {
                val names = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
                val note = names[estimate.noteNumber % 12] + (estimate.noteNumber / 12 - 1)
                Text(stringResource(R.string.pitch_note, note), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.pitch_frequency, format.format(estimate.frequencyHz)))
                Text(stringResource(R.string.pitch_cents, format.format(estimate.cents)))
            } else if (meter.state.status == SoundMeterStatus.READING) Text(stringResource(R.string.pitch_unstable))
            Text(stringResource(R.string.pitch_warning))
            Text(stringResource(R.string.sound_local))
        })
    }
}
