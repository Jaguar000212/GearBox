package com.jaguar.gearbox.ui.tools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlin.random.Random

@Composable
fun FlipCoinScreen(onNavigateBack: () -> Unit) {
    var result by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var flipCount by rememberSaveable { mutableIntStateOf(0) }

    val rotation by animateFloatAsState(
        targetValue = flipCount * 180f,
        animationSpec = tween(durationMillis = 500),
        label = "coinFlip",
    )

    ToolScaffold(
        title = "Flip Coin",
        icon = Tools.byRoute(Tools.ROUTE_FLIP_COIN)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer { rotationY = rotation }
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (result) {
                        null -> "?"
                        true -> "H"
                        false -> "T"
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = when (result) {
                null -> "Tap to flip"
                true -> "Heads!"
                false -> "Tails!"
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                result = Random.nextBoolean()
                flipCount++
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Casino, contentDescription = null)
            Text("  Flip")
        }
    }
}
