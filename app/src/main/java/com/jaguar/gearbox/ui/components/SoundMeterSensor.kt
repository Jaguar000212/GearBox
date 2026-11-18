package com.jaguar.gearbox.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.sensors.*

class SoundMeterHandle(val state: SoundMeterState, val start: () -> Unit, val stop: () -> Unit)
@Composable
fun rememberSoundMeter(): SoundMeterHandle {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(SoundMeterState()) }
    val controller = remember(scope) { SoundMeterController(scope, onState = { state = it }) }
    DisposableEffect(controller, lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.stop()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); controller.stop() }
    }
    return SoundMeterHandle(state, {
        controller.start(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }, controller::stop)
}
