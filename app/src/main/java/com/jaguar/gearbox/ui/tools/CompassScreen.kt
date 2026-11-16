package com.jaguar.gearbox.ui.tools

import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import com.jaguar.gearbox.ui.components.ToolFocusView
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
    fun pointLabel(point: String): String = context.getString(when (point) {
        "N" -> R.string.special_n
        "NE" -> R.string.special_ne
        "E" -> R.string.special_e
        "SE" -> R.string.special_se
        "S" -> R.string.special_s
        "SW" -> R.string.special_sw
        "W" -> R.string.special_w
        "NW" -> R.string.special_nw
        "NNE" -> R.string.special_nne
        "ENE" -> R.string.special_ene
        "ESE" -> R.string.special_ese
        "SSE" -> R.string.special_sse
        "SSW" -> R.string.special_ssw
        "WSW" -> R.string.special_wsw
        "WNW" -> R.string.special_wnw
        else -> R.string.special_nnw
    })
    val historyStore = remember(context) { ToolMemoryStore(context) }
    var captureGeneration by remember { mutableStateOf<Long?>(null) }
    var capturedOutput by remember { mutableStateOf("") }
    var captureId by remember { mutableStateOf("") }
    val history = rememberToolHistory(
        route = Tools.ROUTE_COMPASS,
        inputs = mapOf("source" to context.getString(R.string.special_device_orientation_sensor), "_capture" to captureId),
        output = capturedOutput.takeIf { it.isNotEmpty() },
        expectedHistoryGeneration = captureGeneration,
        onRestore = {}, allowRestore = false, restoreLastResult = false, recordingDelayMillis = 0L,
    )

    var focusView by rememberSaveable { mutableStateOf(false) }
    if (focusView) ToolFocusView(context.getString(R.string.compass_title), onExit = { focusView = false }) {
        OrientationStatusMessage(sensorState.status)
        if (sensorState.status == OrientationStatus.READY) {
            if (snapshot != null) {
                val heading = azimuthDegrees(snapshot.azimuthRad)
                Text(context.getString(R.string.special_compass_focus_heading, pointLabel(compassPoint(heading)), "%.1f".format(heading)), style = MaterialTheme.typography.displayLarge)
                Text(context.getString(R.string.special_magnetic_heading_not_true_north_nearby_metal_and_magnets_can_affe))
            }
        } else {
            Text(context.getString(R.string.special_a_reliable_reading_is_not_available_no_measurement_is_shown))
        }
    }

    ToolScaffold(
        title = stringResource(R.string.compass_title),
        icon = Tools.byRoute(Tools.ROUTE_COMPASS)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
        options = { TextButton(onClick = { focusView = true }) { Text(context.getString(R.string.special_focus_view)) } },
    ) {
        OrientationStatusMessage(sensorState.status)
        if (snapshot == null) return@ToolScaffold

        val heading = azimuthDegrees(snapshot.azimuthRad)
        val point = pointLabel(compassPoint(heading))

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
                Text(context.getString(R.string.special_n), modifier = Modifier.padding(top = 8.dp).align(Alignment.TopCenter), color = labelColor, style = MaterialTheme.typography.labelLarge)
                Text(context.getString(R.string.special_e), modifier = Modifier.padding(end = 8.dp).align(Alignment.CenterEnd), color = labelColor, style = MaterialTheme.typography.labelLarge)
                Text(context.getString(R.string.special_s), modifier = Modifier.padding(bottom = 8.dp).align(Alignment.BottomCenter), color = labelColor, style = MaterialTheme.typography.labelLarge)
                Text(context.getString(R.string.special_w), modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterStart), color = labelColor, style = MaterialTheme.typography.labelLarge)
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
            captureLabel = context.getString(R.string.special_capture_reading),
            enabled = sensorState.status == OrientationStatus.READY,
            capturedOutput = capturedOutput,
            onCapture = {
                captureGeneration = historyStore.historyGeneration(Tools.ROUTE_COMPASS)
                capturedOutput = context.getString(R.string.special_compass_capture, point, "%.1f".format(heading))
                captureId = UUID.randomUUID().toString()
            },
            onClear = { captureGeneration = null; capturedOutput = ""; captureId = "" },
        )
    }
}
