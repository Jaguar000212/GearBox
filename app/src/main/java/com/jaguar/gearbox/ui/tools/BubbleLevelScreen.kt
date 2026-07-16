package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberOrientationSnapshot
import kotlin.math.abs

private const val LEVEL_TOLERANCE_DEGREES = 1.0
private const val BUBBLE_SENSITIVITY = 2.5f

@Composable
fun BubbleLevelScreen(onNavigateBack: () -> Unit) {
    val snapshot = rememberOrientationSnapshot()

    ToolScaffold(
        title = "Bubble Level",
        icon = Tools.byRoute(Tools.ROUTE_BUBBLE_LEVEL)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        if (snapshot == null) {
            Text(
                "Waiting for the orientation sensor... if this doesn't change, your device may not support it.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@ToolScaffold
        }

        val pitchDeg = Math.toDegrees(snapshot.pitchRad.toDouble())
        val rollDeg = Math.toDegrees(snapshot.rollRad.toDouble())
        val isLevel = abs(pitchDeg) < LEVEL_TOLERANCE_DEGREES && abs(rollDeg) < LEVEL_TOLERANCE_DEGREES

        Text(
            text = if (isLevel) "Level!" else "Tilt the device to level it",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            val outlineColor = MaterialTheme.colorScheme.outline
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val bubbleColor = if (isLevel) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }

            Canvas(modifier = Modifier.size(240.dp)) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawCircle(color = trackColor, radius = radius, center = center)
                drawCircle(color = outlineColor, radius = radius, center = center, style = Stroke(width = 3f))
                drawLine(outlineColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1.5f)
                drawLine(outlineColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1.5f)

                val bubbleRadius = radius * 0.16f
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

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Pitch: ${"%.1f".format(pitchDeg)}°   Roll: ${"%.1f".format(rollDeg)}°",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
