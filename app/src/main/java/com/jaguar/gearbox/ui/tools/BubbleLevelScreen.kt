package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.data.ToolMemoryStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberOrientationState
import com.jaguar.gearbox.ui.components.OrientationState
import com.jaguar.gearbox.ui.components.OrientationStatusMessage
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import kotlin.math.abs

private const val LEVEL_TOLERANCE_DEGREES = 1.0
private const val BUBBLE_SENSITIVITY = 2.5f

@Composable
fun BubbleLevelScreen(
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
        route = Tools.ROUTE_BUBBLE_LEVEL,
        inputs = mapOf("source" to "Device orientation sensor", "_capture" to captureId),
        output = capturedOutput.takeIf { it.isNotEmpty() },
        expectedHistoryGeneration = captureGeneration,
        onRestore = {}, allowRestore = false, restoreLastResult = false, recordingDelayMillis = 0L,
    )

    ToolScaffold(
        title = stringResource(R.string.bubble_level_title),
        icon = Tools.byRoute(Tools.ROUTE_BUBBLE_LEVEL)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        OrientationStatusMessage(sensorState.status)
        if (snapshot == null) return@ToolScaffold

        val pitchDeg = Math.toDegrees(snapshot.pitchRad.toDouble())
        val rollDeg = Math.toDegrees(snapshot.rollRad.toDouble())
        val isLevel = abs(pitchDeg) < LEVEL_TOLERANCE_DEGREES && abs(rollDeg) < LEVEL_TOLERANCE_DEGREES

        Text(
            text = stringResource(if (isLevel) R.string.bubble_level_ready else R.string.bubble_level_tilt),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.level_tolerance_help), style = MaterialTheme.typography.bodyMedium)
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
            val bubbleColor = if (isLevel) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }

            val dialDescription = stringResource(if (isLevel) R.string.bubble_level_ready else R.string.bubble_level_tilt)
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f).semantics { contentDescription = dialDescription }) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawCircle(color = trackColor, radius = radius, center = center)
                drawCircle(color = outlineColor, radius = radius, center = center, style = Stroke(width = 3f))
                drawLine(outlineColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1.5f)
                drawLine(outlineColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1.5f)

                val bubbleRadius = radius * 0.16f
                drawCircle(outlineColor, radius = bubbleRadius, center = center, style = Stroke(width = 2.dp.toPx()))
                val maxOffset = radius - bubbleRadius
                val rawOffsetX = rollDeg.toFloat() * BUBBLE_SENSITIVITY * (radius / 100f)
                val rawOffsetY = pitchDeg.toFloat() * BUBBLE_SENSITIVITY * (radius / 100f)
                val offsetMagnitude = kotlin.math.sqrt(rawOffsetX * rawOffsetX + rawOffsetY * rawOffsetY)
                val scale = if (offsetMagnitude > maxOffset && offsetMagnitude > 0f) maxOffset / offsetMagnitude else 1f

                drawCircle(
                    color = bubbleColor,
                    radius = bubbleRadius,
                    center = Offset(center.x + rawOffsetX * scale, center.y - rawOffsetY * scale),
                )
            }
        }

        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.bubble_level_reading, "%.1f".format(pitchDeg), "%.1f".format(rollDeg)),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.level_usage_help), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        DeviceSnapshotActions(
            captureLabel = "Capture reading",
            enabled = sensorState.status == OrientationStatus.READY,
            capturedOutput = capturedOutput,
            onCapture = {
                captureGeneration = historyStore.historyGeneration(Tools.ROUTE_BUBBLE_LEVEL)
                capturedOutput = "Pitch: ${"%.1f".format(pitchDeg)}°; roll: ${"%.1f".format(rollDeg)}°. ${if (isLevel) "Within 1° level tolerance" else "Tilted"}. Device sensor estimate; check against a physical level for precision work."
                captureId = UUID.randomUUID().toString()
            },
            onClear = { captureGeneration = null; capturedOutput = ""; captureId = "" },
        )
    }
}
