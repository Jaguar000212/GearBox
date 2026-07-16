package com.jaguar.gearbox.ui.tools

import android.content.Context
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun FlashlightScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember {
        runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
    var isOn by remember { mutableStateOf(false) }

    // Guards against leaving the torch stuck on if the user navigates away from this screen
    // while it's lit - torch state otherwise persists at the hardware level, not with the screen.
    DisposableEffect(cameraId) {
        onDispose {
            if (isOn && cameraId != null) {
                runCatching { cameraManager.setTorchMode(cameraId, false) }
            }
        }
    }

    ToolScaffold(
        title = "Flashlight",
        icon = Tools.byRoute(Tools.ROUTE_FLASHLIGHT)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        if (cameraId == null) {
            Text(
                "No flash hardware was found on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@ToolScaffold
        }

        Text(
            text = if (isOn) "On" else "Off",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = {
                    val next = !isOn
                    val toggled = runCatching { cameraManager.setTorchMode(cameraId, next) }.isSuccess
                    if (toggled) isOn = next
                },
                modifier = Modifier.size(96.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = if (isOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = if (isOn) "Turn off flashlight" else "Turn on flashlight",
                    tint = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}
