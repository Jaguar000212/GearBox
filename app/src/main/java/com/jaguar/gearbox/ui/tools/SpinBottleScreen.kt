package com.jaguar.gearbox.ui.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SpinBottleScreen(onNavigateBack: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }

    ToolScaffold(
        title = "Spin the Bottle",
        icon = Tools.byRoute(Tools.ROUTE_SPIN_BOTTLE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Bottle",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer { rotationZ = rotation.value },
            )
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (!spinning) {
                    val extraSpins = 360 * (5 + Random.nextInt(3))
                    val finalAngle = Random.nextInt(360)
                    val target = rotation.value + extraSpins + finalAngle
                    scope.launch {
                        spinning = true
                        rotation.animateTo(target, animationSpec = tween(durationMillis = 2500))
                        spinning = false
                    }
                }
            },
            enabled = !spinning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(if (spinning) "  Spinning..." else "  Spin")
        }
    }
}
