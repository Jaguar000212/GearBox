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

class PitchTunerHandle(val state: PitchTunerState, val start: () -> Unit, val stop: () -> Unit)
@Composable
fun rememberPitchTuner(): PitchTunerHandle {
    val context = LocalContext.current
    return rememberPitchTunerWithPermission({
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    })
}

/** Internal fake-source seam for lifecycle tests, not a production permission bypass. */
@Composable
internal fun rememberPitchTunerWithPermission(permission: () -> Boolean, sourceFactory: () -> SoundMeterSource = { AndroidSoundMeterSource() }): PitchTunerHandle {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(PitchTunerState()) }
    val controller = remember(scope) { PitchTunerController(scope, sourceFactory, onState = { state = it }) }
    DisposableEffect(controller, lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.stop()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); controller.stop() }
    }
    return PitchTunerHandle(state, {
        controller.start(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
            permission())
    }, controller::stop)
}
