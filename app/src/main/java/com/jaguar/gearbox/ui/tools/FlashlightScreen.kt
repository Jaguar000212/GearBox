package com.jaguar.gearbox.ui.tools

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.os.Handler
import android.os.Looper
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.jaguar.gearbox.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun FlashlightScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    var attempt by remember { mutableStateOf(0) }
    val discovery = remember(cameraManager, attempt) {
        if (cameraManager == null) null to R.string.flashlight_failed else try {
            val id = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            id to if (id == null) R.string.flashlight_no_hardware else null
        } catch (_: CameraAccessException) {
            null to R.string.flashlight_busy
        } catch (_: SecurityException) {
            null to R.string.flashlight_denied
        } catch (_: RuntimeException) {
            null to R.string.flashlight_failed
        }
    }
    val cameraId = discovery.first
    var isOn by remember { mutableStateOf(false) }
    var errorResource by remember(attempt) { mutableStateOf(discovery.second) }

    DisposableEffect(cameraManager, cameraId, attempt) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) { isOn = enabled; errorResource = null }
            }
            override fun onTorchModeUnavailable(id: String) {
                if (id == cameraId) { isOn = false; errorResource = R.string.flashlight_busy }
            }
        }
        if (cameraId != null) {
            runCatching { cameraManager?.registerTorchCallback(callback, Handler(Looper.getMainLooper())) }
                .onFailure { errorResource = R.string.flashlight_failed }
        }
        onDispose {
            if (isOn && cameraId != null) runCatching { cameraManager?.setTorchMode(cameraId, false) }
            cameraManager?.unregisterTorchCallback(callback)
        }
    }

    ToolScaffold(
        title = stringResource(R.string.flashlight_title),
        icon = Tools.byRoute(Tools.ROUTE_FLASHLIGHT)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        errorResource?.let { resource ->
            Text(
                stringResource(resource),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            OutlinedButton(onClick = { attempt++ }) { Text(stringResource(R.string.flashlight_retry)) }
        }
        if (cameraId == null || cameraManager == null) return@ToolScaffold

        Text(
            text = stringResource(if (isOn) R.string.flashlight_on else R.string.flashlight_off),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = {
                    val next = !isOn
                    try {
                        cameraManager.setTorchMode(cameraId, next)
                        isOn = next
                        errorResource = null
                    } catch (_: CameraAccessException) {
                        errorResource = R.string.flashlight_busy
                    } catch (_: SecurityException) {
                        errorResource = R.string.flashlight_denied
                    } catch (_: RuntimeException) {
                        errorResource = R.string.flashlight_failed
                    }
                },
                modifier = Modifier.size(96.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = if (isOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = stringResource(if (isOn) R.string.flashlight_turn_off else R.string.flashlight_turn_on),
                    tint = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}
