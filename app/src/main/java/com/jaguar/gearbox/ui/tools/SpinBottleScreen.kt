package com.jaguar.gearbox.ui.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SpinBottleScreen(onNavigateBack: () -> Unit) {
    var finalAngle by rememberSaveable { mutableStateOf(-1) }
    val rotation = remember { Animatable(finalAngle.coerceAtLeast(0).toFloat()) }
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }
    val status = when {
        spinning -> "Spinning…"
        finalAngle < 0 -> "Ready to spin"
        else -> "Bottle points $finalAngle° clockwise from the top"
    }

    ToolScaffold(
        title = "Spin the Bottle",
        icon = Tools.byRoute(Tools.ROUTE_SPIN_BOTTLE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text("Place your phone flat between players. The neck of the bottle points to the selected player.",
            style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(status, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).sizeIn(maxWidth = 320.dp, maxHeight = 320.dp),
                    contentAlignment = Alignment.Center) {
                    val ring = MaterialTheme.colorScheme.outlineVariant
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(ring, radius = size.minDimension / 2 - 4.dp.toPx(), style = Stroke(1.dp.toPx()))
                        drawLine(ring, Offset(center.x, 4.dp.toPx()), Offset(center.x, 16.dp.toPx()), 3.dp.toPx())
                    }
                    BottleIllustration(Modifier.fillMaxSize(0.72f).graphicsLayer { rotationZ = rotation.value })
                }
                Spacer(Modifier.height(12.dp))
                Text("The narrow, capped end is the pointer.", style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (!spinning) {
                    // Lock synchronously so rapid taps cannot launch concurrent spins.
                    spinning = true
                    val nextAngle = Random.nextInt(360)
                    val current = ((rotation.value % 360f) + 360f) % 360f
                    val delta = (nextAngle - current + 360f) % 360f
                    val target = current + 360f * (5 + Random.nextInt(3)) + delta
                    scope.launch {
                        try {
                            rotation.snapTo(current)
                            // Animatable honors the system animation-duration scale, including zero.
                            rotation.animateTo(target, animationSpec = tween(durationMillis = 2500))
                            rotation.snapTo(nextAngle.toFloat())
                            finalAngle = nextAngle
                        } finally {
                            spinning = false
                        }
                    }
                }
            },
            enabled = !spinning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (spinning) "Spinning…" else if (finalAngle < 0) "Spin" else "Spin again")
        }
    }
}

/** Theme-aware vector artwork: no bitmap dependency and a clearly distinct pointer end. */
@Composable
private fun BottleIllustration(modifier: Modifier = Modifier) {
    val glass = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.primary
    val label = MaterialTheme.colorScheme.onPrimaryContainer
    Canvas(modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val bottle = Path().apply {
            moveTo(w * .43f, h * .08f)
            lineTo(w * .57f, h * .08f)
            lineTo(w * .57f, h * .32f)
            cubicTo(w * .57f, h * .38f, w * .72f, h * .39f, w * .72f, h * .48f)
            lineTo(w * .72f, h * .84f)
            quadraticBezierTo(w * .72f, h * .92f, w * .64f, h * .92f)
            lineTo(w * .36f, h * .92f)
            quadraticBezierTo(w * .28f, h * .92f, w * .28f, h * .84f)
            lineTo(w * .28f, h * .48f)
            cubicTo(w * .28f, h * .39f, w * .43f, h * .38f, w * .43f, h * .32f)
            close()
        }
        drawPath(bottle, glass)
        drawPath(bottle, outline, style = Stroke(3.dp.toPx()))
        drawRoundRect(outline, Offset(w * .41f, h * .07f), Size(w * .18f, h * .07f))
        drawRoundRect(label.copy(alpha = .14f), Offset(w * .31f, h * .54f), Size(w * .38f, h * .2f))
        val pointer = Path().apply {
            moveTo(w * .5f, h * .56f); lineTo(w * .42f, h * .65f)
            lineTo(w * .58f, h * .65f); close()
        }
        drawPath(pointer, label)
    }
}
