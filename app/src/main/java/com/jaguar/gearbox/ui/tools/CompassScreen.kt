package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.data.ToolMemoryStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jaguar.gearbox.ui.components.OrientationStatus
import com.jaguar.gearbox.ui.components.rememberToolHistory
import java.util.UUID
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.azimuthDegrees
import com.jaguar.gearbox.logic.compassPoint
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberOrientationState
import com.jaguar.gearbox.ui.components.OrientationState
import com.jaguar.gearbox.ui.components.OrientationStatusMessage
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R

@Composable
fun CompassScreen(
    onNavigateBack: () -> Unit,
    sensorState: OrientationState = rememberOrientationState(),
) {
    val snapshot = sensorState.snapshot
    val context = LocalContext.current
    val historyStore = remember(context) { ToolMemoryStore(context) }
    var captureGeneration by remember { mutableStateOf<Long?>(null) }
    var capturedOutput by remember { mutableStateOf("") }
    var captureId by remember { mutableStateOf("") }
    val history = rememberToolHistory(
        route = Tools.ROUTE_COMPASS,
        inputs = mapOf("source" to "Device orientation sensor", "_capture" to captureId),
        output = capturedOutput.takeIf { it.isNotEmpty() },
        expectedHistoryGeneration = captureGeneration,
        onRestore = {}, allowRestore = false, restoreLastResult = false, recordingDelayMillis = 0L,
    )

    ToolScaffold(
        title = stringResource(R.string.compass_title),
        icon = Tools.byRoute(Tools.ROUTE_COMPASS)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        OrientationStatusMessage(sensorState.status)
        if (snapshot == null) return@ToolScaffold

        val heading = azimuthDegrees(snapshot.azimuthRad)
        val point = compassPoint(heading)

        Text(stringResource(R.string.compass_magnetic_label), style = MaterialTheme.typography.labelLarge)
        Text(
            text = stringResource(R.string.compass_heading, point, "%.0f".format(heading)),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.compass_note), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            val outlineColor = MaterialTheme.colorScheme.outline
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

            // The dial rotates opposite the heading so its "N" mark always points magnetic north,
            // like a physical compass card; the needle below stays fixed, pointing at the top of
            // the screen (i.e. wherever the device is currently facing).
            Box(
                modifier = Modifier
                    .fillMaxSize().padding(20.dp).clearAndSetSemantics { }
                    .graphicsLayer { rotationZ = -heading.toFloat() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(color = trackColor, radius = radius, center = center)
                    drawCircle(color = outlineColor, radius = radius, center = center, style = Stroke(width = 3f))
                    for (tick in 0 until 12) {
                        rotate(degrees = tick * 30f, pivot = center) {
                            drawLine(
                                color = outlineColor,
                                start = Offset(center.x, center.y - radius),
                                end = Offset(center.x, center.y - radius + 12f),
                                strokeWidth = 2f,
                            )
                        }
                    }
                }
                Text("N", modifier = Modifier.padding(top = 8.dp).align(Alignment.TopCenter), color = labelColor, style = MaterialTheme.typography.labelLarge)
                Text("E", modifier = Modifier.padding(end = 8.dp).align(Alignment.CenterEnd), color = labelColor, style = MaterialTheme.typography.labelLarge)
                Text("S", modifier = Modifier.padding(bottom = 8.dp).align(Alignment.BottomCenter), color = labelColor, style = MaterialTheme.typography.labelLarge)
                Text("W", modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterStart), color = labelColor, style = MaterialTheme.typography.labelLarge)
            }

            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.compass_facing),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { rotationZ = 180f },
            )
        }
        }
        Text(stringResource(R.string.compass_usage_help), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        DeviceSnapshotActions(
            captureLabel = "Capture reading",
            enabled = sensorState.status == OrientationStatus.READY,
            capturedOutput = capturedOutput,
            onCapture = {
                captureGeneration = historyStore.historyGeneration(Tools.ROUTE_COMPASS)
                capturedOutput = "Magnetic heading: $point ${"%.1f".format(heading)}°. Device sensor estimate, not true north; nearby magnetic fields can affect accuracy."
                captureId = UUID.randomUUID().toString()
            },
            onClear = { captureGeneration = null; capturedOutput = ""; captureId = "" },
        )
    }
}
