package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.jaguar.gearbox.ui.components.rememberOrientationSnapshot

@Composable
fun CompassScreen(onNavigateBack: () -> Unit) {
    val snapshot = rememberOrientationSnapshot()

    ToolScaffold(
        title = "Compass",
        icon = Tools.byRoute(Tools.ROUTE_COMPASS)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        if (snapshot == null) {
            Text(
                "Waiting for the orientation sensor... if this doesn't change, your device may not support it.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@ToolScaffold
        }

        val heading = azimuthDegrees(snapshot.azimuthRad)
        val point = compassPoint(heading)

        Text(
            text = "$point   ${"%.0f".format(heading)}°",
            style = MaterialTheme.typography.headlineMedium,
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
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

            // The dial rotates opposite the heading so its "N" mark always points true north,
            // like a physical compass card; the needle below stays fixed, pointing at the top of
            // the screen (i.e. wherever the device is currently facing).
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer { rotationZ = -heading.toFloat() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(240.dp)) {
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
                contentDescription = "Facing direction",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { rotationZ = 180f },
            )
        }
    }
}
